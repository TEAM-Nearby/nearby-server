// 동행 도메인 모델과 JPA 엔티티 사이의 필드 매핑을 담당하는 클래스
package com.sopt.nearby.companion.adapter.out.persistence.mapper;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostStyleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostStyleEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileStyleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileStyleEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportReasonEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportReasonEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewKeywordEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewKeywordEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCancellationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCheckInEntity;
import com.sopt.nearby.companion.domain.model.CompanionApplication;
import com.sopt.nearby.companion.domain.model.CompanionMatch;
import com.sopt.nearby.companion.domain.model.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.CompanionPost;
import com.sopt.nearby.companion.domain.model.CompanionPostStyle;
import com.sopt.nearby.companion.domain.model.CompanionProfile;
import com.sopt.nearby.companion.domain.model.CompanionProfileStyle;
import com.sopt.nearby.companion.domain.model.CompanionReport;
import com.sopt.nearby.companion.domain.model.CompanionReportReason;
import com.sopt.nearby.companion.domain.model.CompanionReview;
import com.sopt.nearby.companion.domain.model.CompanionReviewKeyword;
import com.sopt.nearby.companion.domain.model.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.MeetingCancellation;
import com.sopt.nearby.companion.domain.model.MeetingCheckIn;

public final class CompanionPersistenceMapper {

	private CompanionPersistenceMapper() {
	}

	public static CompanionProfileEntity toEntity(final CompanionProfile model) {
		return new CompanionProfileEntity(
				model.id(),
				model.userId(),
				model.nickname(),
				model.gender(),
				model.birthYear(),
				model.profileImageUrl(),
				model.intro(),
				model.mannerScore(),
				model.reviewCount(),
				model.status()
		);
	}

	public static CompanionProfile toDomain(final CompanionProfileEntity entity) {
		return new CompanionProfile(
				entity.getId(),
				entity.getUserId(),
				entity.getNickname(),
				entity.getGender(),
				entity.getBirthYear(),
				entity.getProfileImageUrl(),
				entity.getIntro(),
				entity.getMannerScore(),
				entity.getReviewCount(),
				entity.getStatus()
		);
	}

	public static CompanionProfileStyleEntity toEntity(final CompanionProfileStyle model) {
		return new CompanionProfileStyleEntity(model.profileId(), model.keyword());
	}

	public static CompanionProfileStyle toDomain(final CompanionProfileStyleEntity entity) {
		return new CompanionProfileStyle(entity.getProfileId(), entity.getKeyword());
	}

	public static CompanionProfileStyleEntityId toEntityId(final CompanionProfileStyle.Key key) {
		return new CompanionProfileStyleEntityId(key.profileId(), key.keyword());
	}

	public static CompanionPostEntity toEntity(final CompanionPost model) {
		return new CompanionPostEntity(
				model.id(),
				model.hostUserId(),
				model.placeId(),
				model.meetingAt(),
				model.maxParticipants(),
				model.content(),
				model.openChatUrl(),
				model.status(),
				model.createdAt()
		);
	}

	public static CompanionPost toDomain(final CompanionPostEntity entity) {
		return new CompanionPost(
				entity.getId(),
				entity.getHostUserId(),
				entity.getPlaceId(),
				entity.getMeetingAt(),
				entity.getMaxParticipants(),
				entity.getContent(),
				entity.getOpenChatUrl(),
				entity.getStatus(),
				entity.getCreatedAt()
		);
	}

	public static CompanionPostStyleEntity toEntity(final CompanionPostStyle model) {
		return new CompanionPostStyleEntity(model.postId(), model.keyword());
	}

	public static CompanionPostStyle toDomain(final CompanionPostStyleEntity entity) {
		return new CompanionPostStyle(entity.getPostId(), entity.getKeyword());
	}

	public static CompanionPostStyleEntityId toEntityId(final CompanionPostStyle.Key key) {
		return new CompanionPostStyleEntityId(key.postId(), key.keyword());
	}

	public static CompanionApplicationEntity toEntity(final CompanionApplication model) {
		return new CompanionApplicationEntity(
				model.id(),
				model.postId(),
				model.applicantUserId(),
				model.status(),
				model.rejectionReason(),
				model.createdAt()
		);
	}

	public static CompanionApplication toDomain(final CompanionApplicationEntity entity) {
		return new CompanionApplication(
				entity.getId(),
				entity.getPostId(),
				entity.getApplicantUserId(),
				entity.getStatus(),
				entity.getRejectionReason(),
				entity.getCreatedAt()
		);
	}

	public static CompanionMatchEntity toEntity(final CompanionMatch model) {
		return new CompanionMatchEntity(model.id(), model.postId(), model.status(), model.createdAt());
	}

	public static CompanionMatch toDomain(final CompanionMatchEntity entity) {
		return new CompanionMatch(entity.getId(), entity.getPostId(), entity.getStatus(), entity.getCreatedAt());
	}

	public static CompanionMatchParticipantEntity toEntity(final CompanionMatchParticipant model) {
		return new CompanionMatchParticipantEntity(
				model.id(),
				model.matchId(),
				model.userId(),
				model.acceptedApplicationId(),
				model.role()
		);
	}

	public static CompanionMatchParticipant toDomain(final CompanionMatchParticipantEntity entity) {
		return new CompanionMatchParticipant(
				entity.getId(),
				entity.getMatchId(),
				entity.getUserId(),
				entity.getAcceptedApplicationId(),
				entity.getRole()
		);
	}

	public static CompanionMeetingEntity toEntity(final CompanionMeeting model) {
		return new CompanionMeetingEntity(
				model.id(),
				model.matchId(),
				model.status(),
				model.startedAt(),
				model.completedAt()
		);
	}

	public static CompanionMeeting toDomain(final CompanionMeetingEntity entity) {
		return new CompanionMeeting(
				entity.getId(),
				entity.getMatchId(),
				entity.getStatus(),
				entity.getStartedAt(),
				entity.getCompletedAt()
		);
	}

	public static CompanionScheduleEntity toEntity(final CompanionSchedule model) {
		return new CompanionScheduleEntity(
				model.id(),
				model.matchId(),
				model.placeId(),
				model.scheduledAt(),
				model.estimatedDurationMinutes(),
				model.confirmed()
		);
	}

	public static CompanionSchedule toDomain(final CompanionScheduleEntity entity) {
		return new CompanionSchedule(
				entity.getId(),
				entity.getMatchId(),
				entity.getPlaceId(),
				entity.getScheduledAt(),
				entity.getEstimatedDurationMinutes(),
				entity.isConfirmed()
		);
	}

	public static MeetingCheckInEntity toEntity(final MeetingCheckIn model) {
		return new MeetingCheckInEntity(
				model.id(),
				model.meetingId(),
				model.userId(),
				model.latitude(),
				model.longitude(),
				model.checkedInAt()
		);
	}

	public static MeetingCheckIn toDomain(final MeetingCheckInEntity entity) {
		return new MeetingCheckIn(
				entity.getId(),
				entity.getMeetingId(),
				entity.getUserId(),
				entity.getLatitude(),
				entity.getLongitude(),
				entity.getCheckedInAt()
		);
	}

	public static MeetingCancellationEntity toEntity(final MeetingCancellation model) {
		return new MeetingCancellationEntity(
				model.id(),
				model.meetingId(),
				model.canceledByUserId(),
				model.reason(),
				model.canceledAt()
		);
	}

	public static MeetingCancellation toDomain(final MeetingCancellationEntity entity) {
		return new MeetingCancellation(
				entity.getId(),
				entity.getMeetingId(),
				entity.getCanceledByUserId(),
				entity.getReason(),
				entity.getCanceledAt()
		);
	}

	public static CompanionReportEntity toEntity(final CompanionReport model) {
		return new CompanionReportEntity(
				model.id(),
				model.meetingId(),
				model.reporterUserId(),
				model.reportedUserId(),
				model.detail(),
				model.createdAt()
		);
	}

	public static CompanionReport toDomain(final CompanionReportEntity entity) {
		return new CompanionReport(
				entity.getId(),
				entity.getMeetingId(),
				entity.getReporterUserId(),
				entity.getReportedUserId(),
				entity.getDetail(),
				entity.getCreatedAt()
		);
	}

	public static CompanionReportReasonEntity toEntity(final CompanionReportReason model) {
		return new CompanionReportReasonEntity(model.reportId(), model.reason());
	}

	public static CompanionReportReason toDomain(final CompanionReportReasonEntity entity) {
		return new CompanionReportReason(entity.getReportId(), entity.getReason());
	}

	public static CompanionReportReasonEntityId toEntityId(final CompanionReportReason.Key key) {
		return new CompanionReportReasonEntityId(key.reportId(), key.reason());
	}

	public static CompanionReviewEntity toEntity(final CompanionReview model) {
		return new CompanionReviewEntity(
				model.id(),
				model.meetingId(),
				model.reviewerUserId(),
				model.revieweeUserId(),
				model.rating(),
				model.createdAt()
		);
	}

	public static CompanionReview toDomain(final CompanionReviewEntity entity) {
		return new CompanionReview(
				entity.getId(),
				entity.getMeetingId(),
				entity.getReviewerUserId(),
				entity.getRevieweeUserId(),
				entity.getRating(),
				entity.getCreatedAt()
		);
	}

	public static CompanionReviewKeywordEntity toEntity(final CompanionReviewKeyword model) {
		return new CompanionReviewKeywordEntity(model.reviewId(), model.keyword());
	}

	public static CompanionReviewKeyword toDomain(final CompanionReviewKeywordEntity entity) {
		return new CompanionReviewKeyword(entity.getReviewId(), entity.getKeyword());
	}

	public static CompanionReviewKeywordEntityId toEntityId(final CompanionReviewKeyword.Key key) {
		return new CompanionReviewKeywordEntityId(key.reviewId(), key.keyword());
	}
}
