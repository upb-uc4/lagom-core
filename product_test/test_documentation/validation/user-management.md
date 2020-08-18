# User Validation Tests
Every invalid equivalence class returns an error. Every valid class returns an empty sequence.

## Address

### Street (String)
$\textsf{validCharactersStreet} := \textsf{Letters} \cup \textsf{Umlauts} \cup \{\textsf{'ß'}\} \cup \textsf{Digits} \cup \textsf{Whitespace} \cup \{\textsf{'.', ',', '-'}\}$
- vStreet1: street.length $\in$ [1,50] AND street has only characters defined in $\textsf{validCharactersStreet}$
- iStreet1: street.length $\notin$ [1,50] 
- iStreet2: street contains characters not defined in $\textsf{validCharactersStreet}$

### houseNumber (String)
$\textsf{Formats} := \textsf{\{"XX", "XXY", "XXY-Y" | X is a number, Y is a letter\}}$
- vHouseNumber1: houseNumber is of one of the formats given in Formats
- iHouseNumber1: houseNumber is the empty String
- iHouseNumber2: houseNumber has leading zero BUT is of one of the formats given in Formats
- iHouseNumber3: houseNumber is not of one of the formats given in Formats

### zipCode (String)
- vZipCode1: zipCode is number with exactly five digits
- iZipCode1: zipCode is a number with more or less than five digits
- iZipCode2: zipCode is not a number


### city (String)
$\textsf{validCharactersCity} := \textsf{Letters} \cup \textsf{Umlauts} \cup \{\textsf{'ß'}\} \cup \textsf{Digits} \cup \textsf{Whitespace} \cup \{\textsf{'.', ',', '-'}\}$
- vCity1: city.length $\in$ [1,50] AND city has only characters defined in $\textsf{validCharactersCity}$
- iCity1: city.length $\notin$ [1,50] 
- iCity2: city contains characters not defined in $\textsf{validCharactersCity}$

### country (String)
$\begin{aligned} \textsf{Countries} :=\{&\textsf{"Germany", "United States", "Italy", "France", "United Kingdom", "Belgium",} \\
&\textsf{"Netherlands","Spain","Austria", "Switzerland", "Poland"}\}
 \end{aligned}$

- vCountry1: country $\in$ Countries
- iCountry1: country $\notin$ Countries

## User
$\textsf{validCharactersUsername} := \textsf{Letters} \cup \textsf{Digits} \cup \{\textsf{'-'}\}$
### username (String)
- vUsername1: username.length $\in$ [4,16] AND username has only characters defined in $\textsf{validCharactersUsername}$
- iUsername1: username.length $\notin$ [4,16]
- iUsername2: username contains characters not defined in $\textsf{validCharactersUsername}$'

### role (Enum[Role])
$\textsf{Roles} := \{"\textsf{Student}", "\textsf{Lecturer}", "\textsf{Admin"}\}$
- vRole1: role $\in$ Roles AND role conforms to type of object
- iRole1: role $\in$ Roles BUT role does not conform to type of object
- iRole2: role $\notin$ Roles

### address (Address)
Address equivalance classes defined as above.

### firstName (String)
- vFirstName1: firstName.length $\in$ [1,100]
- iFirstName1: firstName.length $\notin$ [1,100]

### lastName (String)
- vLastName1: lastName.length $\in$ [1,100]
- iLastName1: lastName.length $\notin$ [1,100]

### picture (String)
- vPicture1: picture.length $\leq$ 200
- iPicture1: picture.length $>$ 200

### email (String)
- vEmail1: email is of the correct E-Mail format ("example@mail.com")
- iEmail1: email is not of the correct E-Mail format 

### phoneNumber (String)
- vPhoneNumber1: phoneNumber has a 1 to 30 digits and a leading '+'
- iPhoneNumber1: phoneNumber contains symbols other than digits after the '+'
- iPhoneNumber2: phoneNumber is missing the leading '+'
- iPhoneNumber3: phoneNumber.length $\notin$ [2,31]

### birthDate (String)
$\textsf{validBirthdates} := \{"1000-01-01", ..., "9999-12-31" | \textsf{ x is an existing date}\}$
- vBirthDate1: birthDate $\in$ validBirthdates
- iBirthDate1: birthDate $\notin$ validBirthdates BUT is of format YYYY-MM-DD
- iBirthDate2: birthDate is not of the format YYYY-MM-DD

## Admin
Extends User, and inherits parameters as such. No additional parameters.

## Lecturer
Extends User, and inherits parameters as such. Additional parameters:

### freeText (String)
- vFreeText1: freeText.length $\leq$ 10000
- iFreeText1: freeText.length $>$ 10000

### reserachArea (String)
- vResearchArea1: researchArea.length $\leq$ 200
- iResearchArea1: researchArea.length $>$ 200

## Student
Extends User, and inherits parameters as such. Additional parameters:

### latestImmatriculation (String)
$\textsf{Semesters} := \{\textsf{"SS1000"}, \textsf{"WS1000/01"}, ..., \textsf{"SS9998"}, \textsf{"WS9999/00"}\}$
- vLatestImmatriculation1: latestImmatriculation $\in$ Semesters
- vLatestImmatriculation2: latestImmatriculation is the empty String
- iLatestImmatriculation1: latestImmatriculation $\neq$ "" AND latestImmatriculation $\notin$ Semesters

### matriculationId (String)
$\textsf{MatriculationIds} := \{0000001, 0000002, ..., 9999999\}$
- vMatriculationId1: matriculationId $\in$ MatriculationIds
- iMatriculationId1: matriculationId $\notin$ MatriculationIds AND matriculationId.length $=$ 7
- iMatriculationId2: matriculationId.length $\neq$ 7