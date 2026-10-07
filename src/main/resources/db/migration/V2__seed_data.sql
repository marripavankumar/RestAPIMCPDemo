INSERT INTO department (code, name, location, created_at, updated_at) VALUES
    ('ENG',   'Engineering',     'Bengaluru', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('HR',    'Human Resources', 'Hyderabad', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FIN',   'Finance',         'Mumbai',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SALES', 'Sales',           'Pune',      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO employee (first_name, last_name, email, job_title, salary, hire_date, status, department_id, created_at, updated_at) VALUES
    ('Asha',    'Rao',      'asha.rao@example.com',        'Engineering Manager',  185000.00, '2019-04-15', 'ACTIVE',   (SELECT id FROM department WHERE code = 'ENG'),   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Vikram',  'Shah',     'vikram.shah@example.com',     'Senior Engineer',      150000.00, '2020-07-01', 'ACTIVE',   (SELECT id FROM department WHERE code = 'ENG'),   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Meera',   'Iyer',     'meera.iyer@example.com',      'Software Engineer',    110000.00, '2022-01-10', 'ACTIVE',   (SELECT id FROM department WHERE code = 'ENG'),   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Rahul',   'Verma',    'rahul.verma@example.com',     'Software Engineer',    105000.00, '2023-03-20', 'ON_LEAVE', (SELECT id FROM department WHERE code = 'ENG'),   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Priya',   'Nair',     'priya.nair@example.com',      'HR Director',          140000.00, '2018-11-05', 'ACTIVE',   (SELECT id FROM department WHERE code = 'HR'),    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Karan',   'Mehta',    'karan.mehta@example.com',     'Recruiter',             70000.00, '2024-02-12', 'ACTIVE',   (SELECT id FROM department WHERE code = 'HR'),    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Divya',   'Menon',    'divya.menon@example.com',     'Finance Controller',   160000.00, '2017-06-30', 'ACTIVE',   (SELECT id FROM department WHERE code = 'FIN'),   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Arjun',   'Reddy',    'arjun.reddy@example.com',     'Accountant',            85000.00, '2021-09-14', 'ACTIVE',   (SELECT id FROM department WHERE code = 'FIN'),   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Sneha',   'Kulkarni', 'sneha.kulkarni@example.com',  'Financial Analyst',     95000.00, '2022-05-02', 'TERMINATED',(SELECT id FROM department WHERE code = 'FIN'),  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Rohan',   'Gupta',    'rohan.gupta@example.com',     'Head of Sales',        170000.00, '2016-08-22', 'ACTIVE',   (SELECT id FROM department WHERE code = 'SALES'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Ananya',  'Das',      'ananya.das@example.com',      'Account Executive',     90000.00, '2023-10-09', 'ACTIVE',   (SELECT id FROM department WHERE code = 'SALES'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Nikhil',  'Joshi',    'nikhil.joshi@example.com',    'Sales Associate',       65000.00, '2025-01-06', 'ACTIVE',   (SELECT id FROM department WHERE code = 'SALES'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'asha.rao@example.com')    WHERE code = 'ENG';
UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'priya.nair@example.com')  WHERE code = 'HR';
UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'divya.menon@example.com') WHERE code = 'FIN';
UPDATE department SET manager_id = (SELECT id FROM employee WHERE email = 'rohan.gupta@example.com') WHERE code = 'SALES';
