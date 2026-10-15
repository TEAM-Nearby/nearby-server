// 대기 중인 동행 푸시 발송 작업을 처리하는 인바운드 포트다.
package com.sopt.nearby.companion.port.in;

public interface ProcessCompanionPushDeliveriesUseCase {

    void processBatch();
}
