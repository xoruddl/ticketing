-- 예약 행을 넣은 INSERT 문이 DB에서 실행을 시작한 시각. 동시 요청으로 생긴 행들이 서로 몇 ms 차이로 들어왔는지 보려고 둔다.
--
-- 앱이 아니라 DB가 채운다. 엔티티에 필드를 두지 않아도 되고, 앱 시계가 아니라 DB 시계로 찍힌다.
-- MySQL의 current_timestamp 는 행이 실제로 쓰인 순간이나 커밋된 순간이 아니라 그 SQL 문이 시작한 시각이다.
-- 그래서 같은 순간에 시작한 INSERT 들은 µs까지 같은 값을 가질 수 있고, 이 값의 순서가 id(auto_increment) 순서와 다를 수 있다.
--
-- 기존 시각 컬럼과 같은 정밀도(µs)로 둔다. ms 단위로 자르면 가까이 붙은 행들이 더 많이 같은 값으로 뭉친다.
--
-- 주의: 이 값은 DB 세션 시간대를 따르고, expires_at 은 앱의 서울 시계를 따른다.
-- 행끼리의 간격을 재는 용도로만 쓰고, expires_at 과 직접 비교하지 않는다.
--
-- 이미 있는 행에는 컬럼을 추가하는 순간의 시각이 들어간다. 실제 생성 시각이 아니다.
alter table reservation
    add column created_at datetime(6) not null default current_timestamp(6);
