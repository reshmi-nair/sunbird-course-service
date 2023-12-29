# adding column fr
 ALTER TABLE user_enrolments ADD comment text;

# Adding new column
CREATE TABLE IF NOT EXISTS sunbird_courses.batch_user_mapping(batchid text,name text,enrolled_date timestamp,status int,userid text,username text,PRIMARY KEY (batchid, name, enrolled_date, status, userid, username));

CREATE TABLE IF NOT EXISTS sunbird_courses.course_user_mapping(courseid text,name text,enrolled_date timestamp,status int,userid text,username text,PRIMARY KEY (courseid, name, enrolled_date, status, userid, username));


# Alter the table

ALTER TABLE sunbird_courses.batch_user_mapping ADD comment text;

ALTER TABLE sunbird_courses.course_user_mapping ADD comment text;
