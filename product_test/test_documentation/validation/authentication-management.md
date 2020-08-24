# AuthenticationUser Validation Tests
Every invalid equivalence class returns an error. Every valid class returns an empty sequence.

## AuthenticationUser
*validCharactersUsername* := Letters ∪ Digits ∪ {'-'}
### username (String)
- vUsername1: username.length ∈ [4, 16] AND username has only characters defined in *validCharactersUsername*
- iUsername1: username.length ∉ [4, 16]
- iUsername2: username contains characters not defined in *validCharactersUsername*

### password (String)
- vPassword1: password.length > 0
- iPassword1: password is the empty String