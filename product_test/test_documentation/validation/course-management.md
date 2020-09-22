# Course Validation Tests
Every invalid equivalence class returns an error. Every valid class returns an empty sequence.

## Course

### courseId (String)
Not validated, as it is a generated UUID.

### courseName (String)
- vCourseName1: courseName.length ∈ [1, 100]
- iCourseName1: courseName is the empty String
- iCourseName2: courseName.length ∉ 100

### courseType (String)
*validCourseTypes* := {"Lecture", "Seminar", "Project Group"}
- vCourseType1: courseType ∈ *validCourseTypes*
- iCourseType1: courseType ∉ *validCourseTypes*

### startDate (String)
*validDates* := {"1000-01-01", ..., "9999-12-31" | x is an existing date }
- vStartDate1: startDate ∈ *validDates*
- iStartDate1: startDate ∉ *validDates* BUT is of format YYYY-MM-DD
- iStartDate2: startDate is not of the format YYYY-MM-DD

### endDate (String)
- vEndDate1: endDate ∈ *validDates*
- iEndDate1: endDate ∉ *validDates* BUT is of format YYYY-MM-DD
- iEndDate2: endDate is not of the format YYYY-MM-DD

### ects (Int)
- vEcts1: ects ∈ [1, 999]
- iEcts1: ects ∉ [1, 999]

### lecturerId (String)
- vLecturerId1: lecturerId is not the empty String, and lecturerId ∈ ExistingUsernames
- iLecturerId1: lecturerId is the empty String
- iLecturerId2: lecturerId ∉ ExistingUsernames

### maxParticipants (Int)
- vMaxParticipants1: maxParticipants ∈ [1, 9999]
- iMaxParticipants1: maxParticipants ∉ [1, 9999]
  
### currentParticipants (Int)
- vCurrentParticipants1: currentParticipants ∈ [0, maxParticipants]
- iCurrentParticipants1: currentParticipants ∉ [0, maxParticipants]

### courseLanguage (String)
*validLanguages* are "English" and "German"
- vCourseLanguage1: courseLanguage ∈ *validLanguages*
- iCourseLanguage1: courseLanguage ∉ *validLanguages*

### courseDescription (String)
- vCourseDescription1: courseDescription.length ∈ [0, 10000]
- iCourseDescription1: courseDescription.length ∉ [0, 10000]
