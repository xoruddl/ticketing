-- 회차를 뺀다 (DECISIONS.md "도메인: 회차(Schedule)를 뺀다"). V5에서 시작한 전환의 마지막 단계다.
--
-- 엔티티가 더 이상 schedule_id 와 schedule 테이블을 매핑하지 않으므로 지울 수 있다.

-- 회차·좌석 인덱스를 먼저 지운다. 컬럼을 먼저 지우면 MySQL이 이 인덱스를 seat_id 하나짜리로 줄여 남기는데,
-- V5의 idx_reservation_seat 과 같은 모양의 인덱스가 둘이 된다.
alter table reservation
    drop index idx_reservation_schedule_seat;

alter table reservation
    drop column schedule_id;

drop table schedule;
