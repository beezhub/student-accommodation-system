-- GH-225: the arrival term now ranks applicants, so fcfs_weight finally means something.
--
-- Until now OwnerConfigurableScoringStrategy set the arrival score to the maximum constant for
-- every room of every student, so the weight multiplied the same number everywhere and could not
-- influence any ranking. The queue itself was sorted on created_at alone, so none of the weights
-- could influence who was served first either.
--
-- Every component is scored out of 100, so the weights are the whole of the policy. Left at 1.0
-- across the board, the entire span of waiting — a month of it — would be worth exactly the same as
-- one bursary flag, one priority-institution match or one accessibility need. A student who applied
-- first and holds no flags would lose to one who applied yesterday and holds any single flag.
--
-- The decision is that need outranks waiting, and that waiting is a strong tiebreaker between
-- students of comparable need. So the need weights stay at 1.0 against their 100-point flags, and
-- arrival ships at 0.5: a full month of waiting is worth half a flag.
--
-- Existing rows are updated, not just the column default. No tenant has ever expressed an intent
-- through this column, because the value has never affected an outcome — so there is no
-- configuration here to preserve. A tenant who wants a different balance can set one, and that
-- value will now do something.

ALTER TABLE allocation_rules ALTER COLUMN fcfs_weight SET DEFAULT 0.5;

UPDATE allocation_rules SET fcfs_weight = 0.5, updated_at = CURRENT_TIMESTAMP;
