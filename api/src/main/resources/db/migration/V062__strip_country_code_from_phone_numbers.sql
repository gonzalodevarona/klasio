UPDATE students   SET phone        = SUBSTRING(phone        FROM 4) WHERE phone        LIKE '+57%';
UPDATE students   SET tutor_phone  = SUBSTRING(tutor_phone  FROM 4) WHERE tutor_phone  LIKE '+57%';
UPDATE professors SET phone_number = SUBSTRING(phone_number FROM 4) WHERE phone_number LIKE '+57%';
UPDATE users      SET phone_number = SUBSTRING(phone_number FROM 4) WHERE phone_number LIKE '+57%';
