# Course Validation Tests
Every invalid equivalence class returns an error. Every valid class returns an empty sequence.

## Course

### Street (String)
$\textsf{validCharactersStreet} := \textsf{Letters} \cup \textsf{Umlauts} \cup \{\textsf{'ß'}\} \cup \textsf{Digits} \cup \textsf{Whitespace} \cup \{\textsf{'.', ',', '-'}\}$
- vStreet1: street.length $\in$ [1,50] AND street has only characters defined in $\textsf{validCharactersStreet}$
- iStreet1: street.length $\notin$ [1,50] 
- iStreet2: street contains characters not defined in $\textsf{validCharactersStreet}$

### courseId (String)
Not validated, as it is a generated UUID.

### courseName (String)
- vCourseName1: courseName.length $\in$ [1,100]
- iCourseName1: courseName is the empty String
- iCourseName2: courseName.length $>100$

### courseType (String)
validCourseTypes := {"Lecture", "Seminar", "Project Group"}
- vCourseType1: courseType $\in$ validCourseTypes
- iCourseType1: courseType $\notin$ validCourseTypes

### startDate (String)
$\textsf{validDates} := \{"1000-01-01", ..., "9999-12-31" | \textsf{ x is an existing date}\}$
- vStartDate1: startDate $\in$ validDates
- iStartDate1: startDate $\notin$ validDates BUT is of format YYYY-MM-DD
- iStartDate2: startDate is not of the format YYYY-MM-DD

### endDate (String)
- vEndDate1: endDate $\in$ validDates
- iEndDate1: endDate $\notin$ validDates BUT is of format YYYY-MM-DD
- iEndDate2: endDate is not of the format YYYY-MM-DD

### ects (Int)
- vEcts1: ects $\in [1, 999]$
- iEcts1: ects $\notin [1, 999]$

### lecturerId (String)
No explicit validation.

### maxParticipants (Int)
- vMaxParticipants1: maxParticipants $\in [1, 9999]$
- iMaxParticipants1: maxParticipants $\notin [1, 9999]$
  
### currentParticipants (Int)
- vCurrentParticipants1: currentParticipants $\in [0,$ maxParticipants$]$
- iCurrentParticipants1: currentParticipants $\notin [0,$ maxParticipants$]$

### courseLanguage (String)
$\textsf{validLanguages} := \{\textsf{"English"}, \textsf{"German"}\}$
- vCourseLanguage1: courseLanguage $\in$ validLanguages
- iCourseLanguage1: courseLanguage $\notin$ validLanguages

### courseDescription (String)
- vCourseDescription1: courseDescription.length $\in [0, 10000]$
- iCourseDescription1: courseDescription.length $\notin [0, 10000]$
