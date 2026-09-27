-- 결제와 티켓. 예약 테이블에 컬럼으로 붙이지 않고 따로 둔다.
-- Step 4에서 payment·ticket이 각자 모듈이 되면 이 표도 각 모듈이 소유한다. 지금 나눠 두면 그때 표를 쪼갤 필요가 없다.
-- V1·V2와 같은 이유로 외래 키는 두지 않는다. reservation_id 는 값으로만 가리킨다.

-- 결제. 예약 하나에 대해 돈을 받은 기록.
-- Step 0의 결제는 항상 성공하는 내부 처리라, 이 행이 저장되었다는 것 자체가 "결제 성공"이다.
-- 실패·결과 모름 같은 상태는 외부 PG가 들어오는 Step 7에서 더한다.
create table payment
(
    id             bigint      not null auto_increment,
    reservation_id bigint      not null,
    -- 결제한 사용자. 예약의 user_id 와 같지만, 표를 나눈 뒤에도 예약을 보지 않고 "누가 냈는지" 답할 수 있게 둔다.
    user_id        bigint      not null,
    -- 결제 금액(원). 좌석 가격을 결제 시점에 복사해 둔다. 나중에 좌석 가격이 바뀌어도 이미 낸 금액은 움직이지 않는다.
    amount         bigint      not null,
    paid_at        datetime(6) not null,
    primary key (id),
    -- 예약으로 결제를 찾는 인덱스. 여기에 유니크 제약을 두면 이중 결제가 막히지만, 일부러 두지 않는다.
    -- 같은 예약에 결제가 두 번 오는 문제는 동시성(Step 1–2)과 멱등성(Step 7–8)에서 다룬다.
    index idx_payment_reservation (reservation_id)
) engine = InnoDB;

-- 티켓. 확정된 예약에 발급되는 입장권.
create table ticket
(
    id             bigint      not null auto_increment,
    reservation_id bigint      not null,
    -- 입장할 때 보여주는 번호. auto_increment id 를 그대로 쓰면 다음 번호를 추측할 수 있어 따로 둔다. 예: UUID 36자
    code           varchar(36) not null,
    issued_at      datetime(6) not null,
    primary key (id),
    -- 같은 코드가 두 장 나가면 입장에서 구분할 수 없다.
    unique key uk_ticket_code (code),
    -- 예약으로 티켓을 찾는 인덱스. payment 와 같은 이유로 유니크는 두지 않는다.
    index idx_ticket_reservation (reservation_id)
) engine = InnoDB;
