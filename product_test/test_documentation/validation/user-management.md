# User Validation Tests
Every invalid equivalence class returns an error. Every valid class returns an empty sequence.

## Address

### Street (String)
*validCharactersStreet* := Letters ∪ Umlauts ∪ Digits ∪ Whitespace ∪ {ß , .  -}
- vStreet1: street.length ∈ [1, 50] AND street has only valid characters
- iStreet1: street.length ∉ [1, 50] 
- iStreet2: street contains characters not defined in *validCharactersStreet*

### houseNumber (String)
Valid house numbers are of the form "42", "42b", and "53a-c". Letters may be lower- or uppercase.
- vHouseNumber1: houseNumber is of one of the valid formats
- iHouseNumber1: houseNumber is the empty String
- iHouseNumber2: houseNumber has leading zero BUT is of one of a valid formats
- iHouseNumber3: houseNumber is not of one of the valid formats

### zipCode (String)
- vZipCode1: zipCode is number with exactly five digits
- iZipCode1: zipCode is a number with more or less than five digits
- iZipCode2: zipCode is not a number

### city (String)
*validCharactersCity* := Letters ∪ Umlauts ∪ {'ß'} ∪ Digits ∪ Whitespace ∪ {'.', ',', '-'}
- vCity1: city.length ∈ [1,50] AND city has only characters defined in *validCharactersCity*
- iCity1: city.length ∉ [1,50] 
- iCity2: city contains characters not defined in *validCharactersCity*

### country (String)
*Countries* := {"Germany", "United States", "Italy", "France", "United Kingdom", "Belgium", "Netherlands","Spain","Austria", "Switzerland", "Poland"}

- vCountry1: country ∈ Countries
- iCountry1: country ∉ Countries

## User
*validCharactersUsername* := Letters ∪ Digits ∪ {'-'}
### username (String)
- vUsername1: username.length ∈ [4,16] AND username has only characters defined in *validCharactersUsername*
- iUsername1: username.length ∉ [4,16]
- iUsername2: username contains characters not defined in *validCharactersUsername*

### role (Enum[Role])
*Roles* := {"Student", "Lecturer", "Admin"}
- vRole1: role ∈ *Roles* AND role conforms to type of object
- iRole1: role ∈ *Roles* BUT role does not conform to type of object
- iRole2: role ∉ *Roles*

### address (Address)
Address equivalance classes defined as above.

### firstName (String)
- vFirstName1: firstName.length ∈ [1,100]
- iFirstName1: firstName.length ∉ [1,100]

### lastName (String)
- vLastName1: lastName.length ∈ [1,100]
- iLastName1: lastName.length ∉ [1,100]

### picture (String)
- vPicture1: picture.length ≤ 200
- iPicture1: picture.length > 200

### email (String)
- vEmail1: email is of the correct E-Mail format ("example@mail.com")
- iEmail1: email is not of the correct E-Mail format 

### phoneNumber (String)
- vPhoneNumber1: phoneNumber has 1 to 30 digits and a leading '+'
- iPhoneNumber1: phoneNumber contains symbols other than digits after the '+'
- iPhoneNumber2: phoneNumber is missing the leading '+'
- iPhoneNumber3: phoneNumber.length ∉ [2, 31]

### birthDate (String)
*validBirthdates* := {"1000-01-01", ..., "9999-12-31" | x is an existing date }
- vBirthDate1: birthDate ∈ *validBirthdates*
- iBirthDate1: birthDate ∉ *validBirthdates* BUT is of format YYYY-MM-DD
- iBirthDate2: birthDate is not of the format YYYY-MM-DD

## Admin
Extends User, and inherits parameters as such. No additional parameters.

## Lecturer
Extends User, and inherits parameters as such. Additional parameters:

### freeText (String)
- vFreeText1: freeText.length ≤ 10000
- iFreeText1: freeText.length > 10000

### reserachArea (String)
- vResearchArea1: researchArea.length ≤ 200
- iResearchArea1: researchArea.length > 200

## Student
Extends User, and inherits parameters as such. Additional parameters:

### latestImmatriculation (String)
Valid semesters are of the form "SS2016" and "WS2017/18"
- vSemester1: latestImmatriculation in valid format for semesters
- vSemester2: latestImmatriculation is the empty String
- iSemester1: latestImmatriculation is not the empty String AND latestImmatriculation is not in valid format for semesters

### matriculationId (String)
*validMatriculationIds* := {0000001, 0000002, ..., 9999999}
- vMatriculationId1: matriculationId ∈ *validMatriculationIds*
- iMatriculationId1: matriculationId ∉ *validMatriculationIds* AND matriculationId.length = 7
- iMatriculationId2: matriculationId.length ≠ 7