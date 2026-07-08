// 내가 작성한 동행 모집글별 리뷰 키워드 쿼리 결과를 받는 projection이다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

public interface MyCompanionPostKeywordProjection {

	Long getPostId();

	String getKeyword();
}
