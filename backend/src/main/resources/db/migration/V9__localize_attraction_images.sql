-- V2 replaced every attraction cover with a remote image. Point all attraction
-- records at assets packaged with the frontend so page rendering never depends
-- on third-party availability or hotlink policies.
UPDATE tm_attraction
SET cover_img = CONCAT('/images/real/attractions/attraction-', LPAD(id, 2, '0'), '.webp')
WHERE id BETWEEN 1 AND 48;
