# AuthenticationUser Validation Tests
Every invalid equivalence class returns an error. Every valid class returns an empty sequence.

## AuthenticationUser
$\textsf{validCharactersUsername} := \textsf{Letters} \cup \textsf{Digits} \cup \{\textsf{'-'}\}$
### username (String)
- vUsername1: username.length $\in$ [4,16] AND username has only characters defined in $\textsf{validCharactersUsername}$
- iUsername1: username.length $\notin$ [4,16]
- iUsername2: username contains characters not defined in $\textsf{validCharactersUsername}$

### password (String)
- vPassword1: password.length > 0
- iPassword1: password is the empty String