-- Rejecting an application used to leave its waiting-list row WAITING. A row like that keeps the
-- student out of the queue, because only one WAITING row is allowed per student, and the code
-- change only closes rows from now on. Close the ones already stuck.
UPDATE waiting_list_entry w
SET status = 'CANCELLED'
FROM application a
WHERE w.application_id = a.id
  AND w.status = 'WAITING'
  AND a.status IN ('REJECTED', 'EXPIRED', 'CANCELLED');
