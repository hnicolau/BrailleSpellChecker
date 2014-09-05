/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hugonicolau.spellcheckerservice;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.icantrap.collections.dawg.Dawg;
import com.icantrap.collections.dawg.Dawg.SuggestionResult;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hugonicolau.braillespellcheckerservice.R;
import android.hugonicolau.spellcheckerservice.distance.ChordDistances;
import android.hugonicolau.spellcheckerservice.distance.Distance;
import android.service.textservice.SpellCheckerService;
import android.util.Log;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;

public class BrailleSpellCheckerService extends SpellCheckerService {
    
	public static final String TAG = "BrailleSpellCheckerService";
	private static Dawg mDawg = null;
	private static String mPreviousLocale = "";
	private static float[] mFrequencies;
	
    @Override
    public Session createSession() {
    	// this line is key for debugging; however, it makes the thread really slow
    	// use only if it's absolutely necessary
    	//android.os.Debug.waitForDebugger(); 
		
        return new BrailleSpellCheckerSession(this.getApplicationContext());
    }
    
    /**
     * SPELL CHECKER SESSION
     * @author hugonicolau
     *
     */
    private static class BrailleSpellCheckerSession extends Session 
    {    	
    	/**
    	 * Score function, a lower score indicates a better match
    	 * score = a . wMSD(Styped, Sword) + § . Äword
    	 * a, §: weighting factors (derived from pilot data)
    	 * Ä: frequency
    	 * MSD substitution: cost = distance * sw
    	 * MSD insertion: cost = distance * iw
    	 * MSD omissions: cost = ow
    	 */
        
        // empirical values
    	float a = (float) 0.6484113;
    	float b = (float) 0.35158873;
    	float sw = (float) 1.6959325;
    	float iw = (float) 0.41078573;
    	float ow = (float) 1.8156104;
        
    	Context mContext = null;
    	Distance mDistance = ChordDistances.Damerau;
    	
        public BrailleSpellCheckerSession(Context context) 
        {
        	mContext = context;    
        	mContext.registerReceiver(onBroadcast, new IntentFilter(mAction_ConfigureParameters));
        }
        
        @Override
        public void onCreate() 
        {
        	String l = getLocale();
        	// load dawg and frequencies table
        	//println("Started loading ...");
        	//long startTime = System.currentTimeMillis();
        	if(mDawg == null || !mPreviousLocale.equalsIgnoreCase(l))
        	{
        		mPreviousLocale = l;
        		println("loading");
        		load(l);
        		println("loaded");
        	}
        	
        	//long stopTime = System.currentTimeMillis();
    		//println("Load time [" + (stopTime - startTime) + "] ms");
        }
        
        /*
         * LOADING METHODS 
         */
        
        private void load(String l)
        {
        	// load DAWG
        	
        	InputStream is = null;
        	try 
        	{
        		
    	    	if(l.equalsIgnoreCase("en"))
    	    	{
    	    		is = mContext.getResources().openRawResource(R.raw.android_en);
    	    		mDistance = ChordDistances.DamerauEN;
    	    	}
    	    	else if(l.equalsIgnoreCase("pt_PT"))
    	    	{
    	    		is = mContext.getResources().openRawResource(R.raw.android_pt);
    	    		mDistance = ChordDistances.DamerauPT;
    	    	}
    	    	else
    	    	{
    	    		throw new IOException("Invalid locale");
    	    	}
    	    	
        		//long startTime = System.currentTimeMillis();
            	
        		mDawg = Dawg.load(is);
            	
            	//long stopTime = System.currentTimeMillis();
        		//println("Load DAWG time [" + (stopTime - startTime) + "] ms");
    			
    		}
    		catch (IOException ioe) 
    		{
    			// handle this exception
    			println("Couldn't load dawg" + ioe.getMessage());
    		}
    		finally 
    		{
    			IOUtils.closeQuietly(is);
    		}
        	
        	// load frequencies
        	is = null;
        	try 
        	{
        		if(l.equalsIgnoreCase("en"))
    	    	{
    	    		is = mContext.getResources().openRawResource(R.raw.freq_en);
    	    	}
    	    	else if(l.equalsIgnoreCase("pt_PT"))
    	    	{
    	    		is = mContext.getResources().openRawResource(R.raw.freq_pt);
    	    	}
    	    	else
    	    	{
    	    		throw new IOException("Invalid locale");
    	    	}
        		
        		//long startTime = System.currentTimeMillis();
            	
        		mFrequencies = loadFrequencies(is);
            	
            	//long stopTime = System.currentTimeMillis();
            	if(mFrequencies == null) println("Couldn't load frequencies");
            	//else println("Load Frequencies time [" + (stopTime - startTime) + "] ms");
    			
    		}
    		catch (IOException ioe) 
    		{
    			// handle this exception
    			println("Couldn't load frequencies" + ioe.getMessage());
    		}
    		finally 
    		{
    			IOUtils.closeQuietly(is);
    		}
        }
        
        private float[] loadFrequencies(InputStream is) throws StreamCorruptedException, IOException
        {
        	BufferedInputStream bis = new BufferedInputStream (is, 8 * 1024);
    	    ObjectInputStream ois = new ObjectInputStream (bis);

    	    float[] floats = null;
    	
    	    try 
    	    {
    	    	floats = (float[]) ois.readObject ();
    		} 
    	    catch (ClassNotFoundException e) {
    			e.printStackTrace();
    		}
    	    
    	    return floats;
        }
        
        @Override
        public SuggestionsInfo onGetSuggestions(TextInfo textInfo, int suggestionsLimit) 
        {
        	if(textInfo == null) {println("textInfo is null"); return null;}
        	boolean exists = false;
        	
        	// get transcribed word
        	final String word = textInfo.getText();
        	
        	// get suggestions
        	//long startTime = System.currentTimeMillis();
        	int maxCost = 2;
        	if(word.length() > 5)
        	{
        		maxCost = 3;
            	//iw = (float) 1.5;
            	//ow = (float) 2;
        	}
        	
        	//println("Get suggestions for[" + word + "]");
        	
        	Set<SuggestionResult> results = mDawg.searchMSD(word, maxCost, iw, sw, ow, mDistance);
        	
			//long stopTime = System.currentTimeMillis();
			//println("Search time [" + (stopTime - startTime) + "] ms");
        	
			// calculate final score for each word
			//startTime = System.currentTimeMillis();
			for(SuggestionResult result : results)
			{
				if(result.suggestion.equalsIgnoreCase(word))
				{
					// if transcribed word exists, then it reveives the min score
					result.msdScore = -2;
					exists = true;
				}
				else
				{
					result.msdScore = a * (result.msdScore/maxCost) - b * getFrequency(result.suggestion);
				}
			}
			
			if(!exists && mDawg.contains(word)) results.add(mDawg.new SuggestionResult(word, -2));
			
			// blank space filter
			for(int i = 1; i < word.length(); i++)
			{
				String word1 = word.substring(0, i);
				String word2 = word.substring(i);
				if(mDawg.contains(word1) && mDawg.contains(word2))
				{
					SuggestionResult res = mDawg.new SuggestionResult(word1 + " " + word2, ow);
					
					float freq1 = getFrequency(word1);
					float freq2 = getFrequency(word2);
					
					res.msdScore = a * (res.msdScore/maxCost) - b * ((freq1 + freq2) / 2);
					res.msdScore *= 1.5; // penalize for being two words
					results.add(res);
				}
			}
			
			//stopTime = System.currentTimeMillis();
			//println("Scoring time [" + (stopTime - startTime) + "] ms");
			
			SuggestionResult[] resultsArray = results.toArray(new SuggestionResult[results.size()]);
			
			// sort 
			//startTime = System.currentTimeMillis();
			Quicksort sorter = new Quicksort();
			sorter.sort(resultsArray);
			//stopTime = System.currentTimeMillis();
			//println("Sort time [" + (stopTime - startTime) + "] ms");
			
			//if(results.size() == 0) println("Empty results for word[" + word + "]");
        	return new SuggestionsInfo(0, Dawg.extractWords(resultsArray, suggestionsLimit));
        }
        
        /* COMMUNICATION WITH EXTERNAL APPS */
        String mAction_ConfigureParameters = "spellchecker_configureparameters";
        String mKey_ConfigureParameters = "spellchecker_parameters";
    	String mAction_ConfirmParameters = "spellchecker_configure";
    	String mKey_ConfirmParameters = "spellchecker_parametersresult";
    	
        private final BroadcastReceiver onBroadcast = new BroadcastReceiver() {
    		
    		@Override
    		public void onReceive(Context context, Intent intent) 
    		{
    			// spellchecker commands
    			if(intent.getAction().equalsIgnoreCase(mAction_ConfigureParameters))
    			{
    				// read parameters
    				// [0] alpha
    				// [1] beta
    				// [2] insertion cost
    				// [3] substitution cost
    				// [4] omission cost
    				
    				float[] parameters = intent.getExtras().getFloatArray(mKey_ConfigureParameters);
    				Log.v(TAG, "received configuration message");
    				if(parameters == null) return;
    				
    				if(parameters.length == 5)
    				{
    					// change spellchecker parameters
    					a = parameters[0];
    					b = parameters[1];
    					iw = parameters[2];
    					sw = parameters[3];
    					ow = parameters[4];
    					
    					// send confirmation
    					Intent command = new Intent(mAction_ConfirmParameters);
    		            command.putExtra(mKey_ConfirmParameters, "valid");
    		            mContext.sendBroadcast(command);
    				}
    				else
    				{
    					// invalid number of parameters
    					Intent command = new Intent(mAction_ConfirmParameters);
    		            command.putExtra(mKey_ConfirmParameters, "invalid");
    		            mContext.sendBroadcast(command);
    				}
    					
    			}
    		}
    	};
        
        /* UTILS */
        private float getFrequency(String word)
        {        	
        	int hash = mDawg.wordToHash(word);
        	//println("Word[" + word + "] Hash[" + hash + "] Freq[" + mFrequencies[hash] + "]");
        	return mFrequencies[hash];
        }
        
        public class Quicksort  {
      	  private SuggestionResult[] data;
      	  private int number;

      	  public void sort(SuggestionResult[] values) {
      	    // check for empty or null array
      	    if (values == null || values.length == 0){
      	      return;
      	    }
      	    this.data = values;
      	    number = values.length;
      	    quicksort(0, number - 1);
      	  }

      	  private void quicksort(int low, int high) {
      	    int i = low, j = high;
      	    // Get the pivot element from the middle of the list
      	    SuggestionResult pivot = data[low + (high-low)/2];

      	    // Divide into two lists
      	    while (i <= j) {
      	      // If the current value from the left list is smaller then the pivot
      	      // element then get the next element from the left list
      	      while (data[i].msdScore < pivot.msdScore) {
      	        i++;
      	      }
      	      // If the current value from the right list is larger then the pivot
      	      // element then get the next element from the right list
      	      while (data[j].msdScore > pivot.msdScore) {
      	        j--;
      	      }

      	      // If we have found a values in the left list which is larger then
      	      // the pivot element and if we have found a value in the right list
      	      // which is smaller then the pivot element then we exchange the
      	      // values.
      	      // As we are done we can increase i and j
      	      if (i <= j) {
      	        exchange(i, j);
      	        i++;
      	        j--;
      	      }
      	    }
      	    // Recursion
      	    if (low < j)
      	      quicksort(low, j);
      	    if (i < high)
      	      quicksort(i, high);
      	  }

      	  private void exchange(int i, int j) {
      	    SuggestionResult temp = data[i];
      	    data[i] = data[j];
      	    data[j] = temp;
      	  }
      }
    }
    
    /* UTILS */
    
    public static void println(String message)
	{
		Log.v(TAG, message);
	}
    
    
}
