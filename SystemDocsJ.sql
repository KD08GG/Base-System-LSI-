-- Create and use the database
CREATE DATABASE SystemDocs1;
USE SystemDocs1;

----------------------------------------------------------------------------------------

-- Table: TEXT
CREATE TABLE TEXT (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    url VARCHAR(255) NOT NULL,
    title VARCHAR(255)
);

-- Table: TERM
CREATE TABLE TERM (
    name VARCHAR(100) PRIMARY KEY
);

-- Table: HAS
CREATE TABLE HAS (
    text_id INTEGER,
    term_name VARCHAR(100),
    frequency REAL NOT NULL,
    PRIMARY KEY (text_id, term_name),
    FOREIGN KEY (text_id) REFERENCES TEXT(id) ON DELETE CASCADE,
    FOREIGN KEY (term_name) REFERENCES TERM(name) ON DELETE CASCADE
);

-- Table: QUERY
CREATE TABLE QUERY (
    label VARCHAR(100) PRIMARY KEY,
    content TEXT NOT NULL
);

-- Table: QUERY_TERM
CREATE TABLE QUERY_TERM (
    query_label VARCHAR(100),
    term_name VARCHAR(100),
    frequency REAL NOT NULL,
    PRIMARY KEY (query_label, term_name),
    FOREIGN KEY (query_label) REFERENCES QUERY(label) ON DELETE CASCADE,
    FOREIGN KEY (term_name) REFERENCES TERM(name) ON DELETE CASCADE
);

-- Optional: COMPLETE view
CREATE VIEW COMPLETE AS
SELECT text_id, term_name, frequency FROM HAS
UNION
SELECT t.id AS text_id, tr.name AS term_name, 0 AS frequency
FROM TEXT t
CROSS JOIN TERM tr
WHERE (t.id, tr.name) NOT IN (SELECT text_id, term_name FROM HAS);

----------------------------------------------------------------------------------------

-- Begin transaction
START TRANSACTION;

-- Step 1: Insert TERMS extracted from the 3 texts
INSERT INTO TERM (name) VALUES
('technology'),
('culture'),
('expression'),
('platform'),
('virtual'),
('heritage'),
('social'),
('media'),
('preservation'),
('identity'),
('diversity'),
('sports'),
('performance'),
('wearable'),
('biometrics'),
('training'),
('broadcast'),
('fairness'),
('e-sports'),
('privacy'),
('interaction'),
('communication'),
('inclusion'),
('misinformation'),
('addiction'),
('literacy'),
('algorithm');

-- Step 2: Insert TEXT entries
INSERT INTO TEXT (id, url, title) VALUES
(1, 'C:\\Docs\\doc1.txt', 'Technology and Culture'),
(2, 'C:\\Docs\\doc2.txt', 'Technology in Sports'),
(3, 'C:\\Docs\\doc3.txt', 'Technology and Social Interaction');

-- Step 3: Insert HAS entries (frequencies estimated based on content emphasis)
-- Document 1: Technology and Culture
INSERT INTO HAS (text_id, term_name, frequency) VALUES
(1, 'technology', 10),
(1, 'culture', 9),
(1, 'expression', 3),
(1, 'platform', 4),
(1, 'virtual', 2),
(1, 'heritage', 2),
(1, 'social', 4),
(1, 'media', 3),
(1, 'preservation', 2),
(1, 'identity', 2),
(1, 'diversity', 2);

-- Document 2: Technology in Sports
INSERT INTO HAS (text_id, term_name, frequency) VALUES
(2, 'technology', 10),
(2, 'sports', 9),
(2, 'performance', 4),
(2, 'wearable', 2),
(2, 'biometrics', 2),
(2, 'training', 3),
(2, 'broadcast', 2),
(2, 'fairness', 2),
(2, 'e-sports', 2),
(2, 'privacy', 2);

-- Document 3: Technology and Social Interaction
INSERT INTO HAS (text_id, term_name, frequency) VALUES
(3, 'technology', 10),
(3, 'interaction', 4),
(3, 'communication', 3),
(3, 'social', 9),
(3, 'media', 3),
(3, 'inclusion', 2),
(3, 'misinformation', 2),
(3, 'addiction', 2),
(3, 'literacy', 2),
(3, 'algorithm', 2);

----------------------------------------------------------------------------------------

-- Step 4: Example QUERY
INSERT INTO QUERY (label, content)
VALUES ('Q1', 'How does technology influence culture and society?');

INSERT INTO QUERY_TERM (query_label, term_name, frequency)
VALUES
('Q1', 'technology', 1),
('Q1', 'culture', 1),
('Q1', 'society', 1),
('Q1', 'influence', 1);

-- Similarity Queries 
-- Cosine Similarity Between Two Documents
SELECT d.text_id,
       SUM(d.frequency * q.frequency) / (
           SQRT(SUM(d.frequency * d.frequency)) *
           SQRT(SUM(q.frequency * q.frequency))
       ) AS cosine_similarity
FROM COMPLETE d
JOIN COMPLETE q ON d.term_name = q.term_name AND q.text_id = 1
GROUP BY d.text_id
ORDER BY cosine_similarity DESC;


-- Dissimilarity Queries
-- Euclidean Distance Between Query and All Documents

SELECT td.text_id,
       SQRT(SUM(POWER(td.frequency - tq.frequency, 2))) AS euclidean_distance
FROM COMPLETE tq
JOIN COMPLETE td ON tq.term_name = td.term_name
WHERE tq.text_id = 1
GROUP BY td.text_id
ORDER BY euclidean_distance ASC;

-- Commit transaction
COMMIT;