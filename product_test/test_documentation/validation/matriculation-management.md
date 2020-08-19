# MatriculationData Validation Tests
Every invalid equivalence class returns an error. Every valid class returns an empty sequence.

## AuthenticationUser
$\textsf{validCharactersUsername} := \textsf{Letters} \cup \textsf{Digits} \cup \{\textsf{'-'}\}$
### fieldOfStudy (String)
$\begin{aligned}\textsf{validFieldsOfStudies} := \{
&\textsf{"Computer Science", "Philosophy", "Media Sciences", "Economics",}\\
&\textsf{"Mathematics", "Physics","Chemistry", "Education", "Sports Science",}\\
&\textsf{"Japanology", "Spanish Culture", "Pedagogy", "Business Informatics",}\\
&\textsf{"Linguistics"}
\}\end{aligned}$
- vFieldOfStudy1: fieldOfStudy $\in$ validFieldsOfStudies
- vFieldOfStudy1: fieldOfStudy $\notin$ validFieldsOfStudies

### semester (String)
$\textsf{Semesters} := \{\textsf{"SS1000"}, \textsf{"WS1000/01"}, ..., \textsf{"SS9998"}, \textsf{"WS9999/00"}\}$
- vSemester1: latestImmatriculation $\in$ Semesters
- vSemester2: latestImmatriculation is the empty String
- iSemester1: latestImmatriculation $\neq$ "" AND latestImmatriculation $\notin$ Semesters