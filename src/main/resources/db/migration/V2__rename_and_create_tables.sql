-- ──────────────────────────────────────────────────────────────────────────────
-- V2__rename_and_create_tables.sql
-- Rename V1 tables to UPPER_SNAKE_CASE and create all new tables
-- ──────────────────────────────────────────────────────────────────────────────

-- ── 1. Rename existing V1 tables to match entity @Table names ─────────────────

-- Drop FK constraints before renaming (MySQL requires this)
ALTER TABLE user_preferences DROP FOREIGN KEY fk_pref_user;
ALTER TABLE meeting_feedback  DROP FOREIGN KEY fk_feedback_meeting;
ALTER TABLE meeting_feedback  DROP FOREIGN KEY fk_feedback_user;

RENAME TABLE companies        TO COMPANIES;
RENAME TABLE users            TO USERS;
RENAME TABLE user_preferences TO USER_PREFERENCES;
RENAME TABLE meetings         TO MEETING;
RENAME TABLE meeting_feedback TO MEETING_FEEDBACK;

-- Re-add FK constraints with new table names
ALTER TABLE USER_PREFERENCES
    ADD CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES USERS (id);

ALTER TABLE MEETING_FEEDBACK
    ADD CONSTRAINT fk_feedback_meeting FOREIGN KEY (meeting_id) REFERENCES MEETING (id),
    ADD CONSTRAINT fk_feedback_user    FOREIGN KEY (user_id)    REFERENCES USERS (id);

-- ── 2. Add cognitoSub to MEETING_FEEDBACK (replaces userId FK) ───────────────
ALTER TABLE MEETING_FEEDBACK
    ADD COLUMN cognitoSub VARCHAR(100) AFTER user_id;

-- ── 3. MATCH_RESULT (was 'matches' with different structure) ─────────────────
RENAME TABLE matches TO MATCH_RESULT;

ALTER TABLE MATCH_RESULT
    ADD COLUMN cognitoSubA      VARCHAR(100) NOT NULL DEFAULT '' AFTER id,
    ADD COLUMN cognitoSubB      VARCHAR(100) NOT NULL DEFAULT '' AFTER cognitoSubA,
    ADD COLUMN matchCategory    VARCHAR(60)  NOT NULL DEFAULT '' AFTER cognitoSubB,
    ADD COLUMN compatibilityScore DOUBLE     AFTER matchCategory,
    ADD COLUMN scoreBreakdown   JSON         AFTER compatibilityScore,
    ADD COLUMN isMutual         TINYINT(1)   DEFAULT 0 AFTER status,
    ADD COLUMN shownAt          DATETIME     AFTER isMutual,
    ADD COLUMN actedAt          DATETIME     AFTER shownAt,
    ADD COLUMN roundCount       INT          AFTER actedAt,
    ADD COLUMN maxRounds        INT          AFTER roundCount;

-- ── 4. BASE_USER_PROFILES ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS BASE_USER_PROFILES (
    id               INT          NOT NULL AUTO_INCREMENT,
    userId           VARCHAR(100) NOT NULL UNIQUE,
    email            VARCHAR(150) NOT NULL UNIQUE,
    displayName      VARCHAR(100) NOT NULL,
    dateOfBirth      DATE         NOT NULL,
    gender           VARCHAR(20)  NOT NULL,
    currentCity      VARCHAR(100),
    currentState     VARCHAR(100),
    currentCountry   VARCHAR(100),
    latitude         DOUBLE,
    longitude        DOUBLE,
    profilePhotoUrl  VARCHAR(500),
    isPhotoVerified  TINYINT(1)   DEFAULT 0,
    tagline          VARCHAR(200),
    aboutMe          TEXT,
    languagesKnown   VARCHAR(300),
    isProfileVerified TINYINT(1)  DEFAULT 0,
    isActive         TINYINT(1)   DEFAULT 1,
    lastActiveAt     DATETIME,
    createdAt        DATETIME,
    updatedAt        DATETIME,
    PRIMARY KEY (id),
    INDEX idx_bup_cognito (userId)
);

-- ── 5. CATEGORY_PROFILE_REGISTRY ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS CATEGORY_PROFILE_REGISTRY (
    id                    INT         NOT NULL AUTO_INCREMENT,
    cognitoSub            VARCHAR(100) NOT NULL,
    matchCategory         VARCHAR(60)  NOT NULL,
    preferredGender       VARCHAR(20),
    preferredCity         VARCHAR(100),
    preferredState        VARCHAR(100),
    preferredCountry      VARCHAR(100),
    maxTimezoneOffsetHours INT,
    sameCompanyAllowed    TINYINT(1)   DEFAULT 0,
    completionPct         INT          DEFAULT 0,
    isActive              TINYINT(1)   DEFAULT 1,
    createdAt             DATETIME,
    updatedAt             DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_cpr_user_category (cognitoSub, matchCategory),
    INDEX idx_cpr_cognito (cognitoSub)
);

-- ── 6. USER_ADDRESS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS USER_ADDRESS (
    id            INT          NOT NULL AUTO_INCREMENT,
    cognitoSub    VARCHAR(100) NOT NULL,
    streetAddress VARCHAR(500),
    city          VARCHAR(100),
    state         VARCHAR(100),
    country       VARCHAR(100) NOT NULL,
    zip           VARCHAR(20),
    createdAt     DATETIME,
    updatedAt     DATETIME,
    PRIMARY KEY (id),
    INDEX idx_addr_cognito (cognitoSub)
);

-- ── 7. BLOCK_LIST ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS BLOCK_LIST (
    id        INT          NOT NULL AUTO_INCREMENT,
    blockerId INT          NOT NULL,
    blockedId INT          NOT NULL,
    reason    VARCHAR(200),
    blockedAt DATETIME,
    createdAt DATETIME,
    updatedAt DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_block (blockerId, blockedId)
);

-- ── 8. USER_PREFERENCES ─ add missing columns ────────────────────────────────
ALTER TABLE USER_PREFERENCES
    ADD COLUMN IF NOT EXISTS minAge            INT,
    ADD COLUMN IF NOT EXISTS maxAge            INT,
    ADD COLUMN IF NOT EXISTS preferredGender   VARCHAR(20),
    ADD COLUMN IF NOT EXISTS preferredCompany  VARCHAR(200),
    ADD COLUMN IF NOT EXISTS preferredCollege  VARCHAR(200),
    ADD COLUMN IF NOT EXISTS preferredZip      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS preferredCity     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS preferredState    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS preferredCountry  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS sameCompanyAllowed TINYINT(1) DEFAULT 0;

-- ── 9. PARTNER_PREFERENCES ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS PARTNER_PREFERENCES (
    id                  INT           NOT NULL AUTO_INCREMENT,
    cognitoSub          VARCHAR(100),
    ageMin              INT,
    ageMax              INT,
    heightMinCm         INT,
    heightMaxCm         INT,
    maritalStatusPref   VARCHAR(200),
    preferredCountries  VARCHAR(300),
    preferredStates     VARCHAR(300),
    openToRelocation    TINYINT(1)    DEFAULT 0,
    religionPref        VARCHAR(200),
    castePref           VARCHAR(300),
    motherTonguePref    VARCHAR(300),
    dietaryPref         VARCHAR(200),
    educationPref       VARCHAR(300),
    employmentTypePref  VARCHAR(200),
    incomeMinInr        DECIMAL(15,2),
    incomeMaxInr        DECIMAL(15,2),
    smokingPref         VARCHAR(100),
    drinkingPref        VARCHAR(100),
    bodyTypePref        VARCHAR(200),
    familyTypePref      VARCHAR(100),
    familyValuesPref    VARCHAR(100),
    manglikPref         VARCHAR(100),
    horoscopeMatchRequired TINYINT(1) DEFAULT 0,
    aboutPartner        TEXT,
    createdAt           DATETIME,
    updatedAt           DATETIME,
    PRIMARY KEY (id)
);

-- ── 10. EXT_MATRIMONIAL_PROFILES ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS EXT_MATRIMONIAL_PROFILES (
    id                  INT          NOT NULL AUTO_INCREMENT,
    cognitoSub          VARCHAR(100) NOT NULL UNIQUE,
    religion            VARCHAR(50),
    caste               VARCHAR(100),
    subCaste            VARCHAR(100),
    gotram              VARCHAR(100),
    motherTongue        VARCHAR(60),
    dietaryHabits       VARCHAR(30),
    highestEducation    VARCHAR(100),
    educationDetail     VARCHAR(200),
    profession          VARCHAR(100),
    employerName        VARCHAR(150),
    employmentType      VARCHAR(40),
    annualIncomeInr     DECIMAL(15,2),
    nativeCity          VARCHAR(100),
    nativeState         VARCHAR(100),
    familyType          VARCHAR(30),
    familyValues        VARCHAR(30),
    familyStatus        VARCHAR(40),
    fatherOccupation    VARCHAR(150),
    motherOccupation    VARCHAR(150),
    siblingsCount       INT,
    siblingsDetail      VARCHAR(300),
    heightCm            INT,
    maritalStatus       VARCHAR(30),
    bodyType            VARCHAR(30),
    complexion          VARCHAR(30),
    smokingHabit        VARCHAR(20),
    drinkingHabit       VARCHAR(20),
    manglikStatus       VARCHAR(20),
    rashi               VARCHAR(50),
    nakshatra           VARCHAR(60),
    birthPlace          VARCHAR(150),
    birthTime           TIME,
    horoscopeMatchRequired TINYINT(1) DEFAULT 0,
    partnerPrefId       INT,
    createdAt           DATETIME,
    updatedAt           DATETIME,
    PRIMARY KEY (id),
    INDEX idx_matrimonial_cognito (cognitoSub)
);

-- ── 11. EXT_DATING_PROFILES ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS EXT_DATING_PROFILES (
    id                  INT          NOT NULL AUTO_INCREMENT,
    cognitoSub          VARCHAR(100) NOT NULL UNIQUE,
    dietaryHabits       VARCHAR(30),
    smokingHabit        VARCHAR(20),
    drinkingHabit       VARCHAR(20),
    heightCm            INT,
    bodyType            VARCHAR(30),
    relationshipGoal    VARCHAR(50),
    sexualOrientation   VARCHAR(50),
    hasChildren         TINYINT(1),
    wantsChildren       TINYINT(1),
    loveLanguage        VARCHAR(50),
    personalityType     VARCHAR(20),
    interestTags        VARCHAR(500),
    promptQuestion1     VARCHAR(200),
    promptAnswer1       TEXT,
    promptQuestion2     VARCHAR(200),
    promptAnswer2       TEXT,
    prefAgeMin          INT,
    prefAgeMax          INT,
    prefGenders         VARCHAR(100),
    prefHeightMinCm     INT,
    prefRelationshipGoal VARCHAR(50),
    createdAt           DATETIME,
    updatedAt           DATETIME,
    PRIMARY KEY (id),
    INDEX idx_dating_cognito (cognitoSub)
);

-- ── 12. EXT_FITNESS_PROFILES ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS EXT_FITNESS_PROFILES (
    id                   INT          NOT NULL AUTO_INCREMENT,
    cognitoSub           VARCHAR(100) NOT NULL UNIQUE,
    fitnessActivities    VARCHAR(300),
    fitnessLevel         VARCHAR(30),
    workoutDays          VARCHAR(100),
    preferredWorkoutTime VARCHAR(50),
    gymName              VARCHAR(150),
    isOkWithMixedGender  TINYINT(1),
    sportsLeagueLevel    VARCHAR(30),
    fitnessGoal          VARCHAR(100),
    dietPreference       VARCHAR(50),
    createdAt            DATETIME,
    updatedAt            DATETIME,
    PRIMARY KEY (id),
    INDEX idx_fitness_cognito (cognitoSub)
);

-- ── 13. EXT_FLATMATE_PROFILES ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS EXT_FLATMATE_PROFILES (
    id                     INT          NOT NULL AUTO_INCREMENT,
    cognitoSub             VARCHAR(100) NOT NULL UNIQUE,
    lookingIn              VARCHAR(200),
    budgetRangeInr         VARCHAR(50),
    moveInDate             DATE,
    preferredFlatmateGender VARCHAR(20),
    occupationType         VARCHAR(50),
    isVegetarianHousehold  TINYINT(1),
    allowsSmoking          TINYINT(1),
    hasPets                TINYINT(1),
    allowsPets             TINYINT(1),
    sleepSchedule          VARCHAR(30),
    cleanlinessLevel       VARCHAR(30),
    guestsPolicy           VARCHAR(30),
    hasCurrentFlat         TINYINT(1),
    currentFlatDetails     TEXT,
    createdAt              DATETIME,
    updatedAt              DATETIME,
    PRIMARY KEY (id),
    INDEX idx_flatmate_cognito (cognitoSub)
);

-- ── 14. EXT_GAMING_PROFILES ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS EXT_GAMING_PROFILES (
    id                 INT          NOT NULL AUTO_INCREMENT,
    cognitoSub         VARCHAR(100) NOT NULL UNIQUE,
    platforms          VARCHAR(100),
    favoriteGames      VARCHAR(500),
    favoriteGenres     VARCHAR(200),
    gamingSchedule     VARCHAR(100),
    skillLevel         VARCHAR(30),
    communicationStyle VARCHAR(30),
    isOkWithNewbies    TINYINT(1),
    gamertags          JSON,
    createdAt          DATETIME,
    updatedAt          DATETIME,
    PRIMARY KEY (id),
    INDEX idx_gaming_cognito (cognitoSub)
);

-- ── 15. EXT_PROFESSIONAL_PROFILES ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS EXT_PROFESSIONAL_PROFILES (
    id                  INT          NOT NULL AUTO_INCREMENT,
    cognitoSub          VARCHAR(100) NOT NULL UNIQUE,
    currentRole         VARCHAR(100),
    currentCompany      VARCHAR(150),
    yearsOfExperience   INT,
    industryDomain      VARCHAR(100),
    techStack           TEXT,
    skillsOffering      TEXT,
    skillsSeeking       TEXT,
    mentorshipRole      VARCHAR(30),
    openToCoFounder     TINYINT(1),
    startupIdeas        TEXT,
    linkedinUrl         VARCHAR(300),
    githubUrl           VARCHAR(300),
    portfolioUrl        VARCHAR(300),
    certifications      TEXT,
    preferredCollabMode VARCHAR(30),
    availabilitySlots   TEXT,
    createdAt           DATETIME,
    updatedAt           DATETIME,
    PRIMARY KEY (id),
    INDEX idx_professional_cognito (cognitoSub)
);

-- ── 16. EXT_TRAVEL_PROFILES ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS EXT_TRAVEL_PROFILES (
    id                    INT          NOT NULL AUTO_INCREMENT,
    cognitoSub            VARCHAR(100) NOT NULL UNIQUE,
    travelStyle           VARCHAR(50),
    preferredDestinations VARCHAR(500),
    bucketListPlaces      VARCHAR(500),
    tripsPerYear          INT,
    preferredTripDuration VARCHAR(30),
    hasTraveledAbroad     TINYINT(1),
    countriesVisited      INT,
    dietaryNeeds          VARCHAR(50),
    isOkWithBudgetStays   TINYINT(1),
    isOkWithCamping       TINYINT(1),
    preferredGroupSize    VARCHAR(30),
    upcomingTrips         TEXT,
    pastTripsHighlights   TEXT,
    createdAt             DATETIME,
    updatedAt             DATETIME,
    PRIMARY KEY (id),
    INDEX idx_travel_cognito (cognitoSub)
);
