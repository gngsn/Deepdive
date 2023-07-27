package com.gngsn.account.domain;

/**
 * Activity Entity.
 * - 계좌에 대한 모든 입금과 출금 포착.
 * - 한 계좌에 대한 모든 활동(activity)들을 항상 메모리에 한꺼번에 올리는 것은 효율적이지 않기 때문에, Account 엔티티는 ActiviryWindow 값 객체(value object)에서 포착한 지난 며칠 혹은 몇 주간의 범위에 해당하는 활동만 보유
 */
public class Activity {

}
