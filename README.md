BrailleSpellChecker
===================

B# is a correction system for multitouch Braille input that resorts to the chord itself as the basis for similarity analysis between words. This means that even partially correct or invalid chords can be used to retrieve better matches. Even non-alphabetic characters, which are usually ignored by traditional spellcheckers, can provide useful information.We extended the Damerau-Levenshtein distance to assess proximity between chords, and thus use this information to search for the most probable corrections. The system runs on mainstream Android smartphones.
