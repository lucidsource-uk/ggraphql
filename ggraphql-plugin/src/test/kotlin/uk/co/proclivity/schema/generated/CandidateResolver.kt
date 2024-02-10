package uk.co.proclivity.schema.generated

import kotlin.Int
import kotlin.String

public interface CandidateResolver {
  public fun audits(pageSize: Int?, cursor: String?): PaginatedResult<ChangeLog>

  public fun persistCandidate(candidate: CandidateInput): Candidate

  public fun deleteCandidate(candidateId: String): SuccessResponse

  public fun candidate(candidateId: String): Candidate?

  public fun candidates(
    `where`: CandidateFilter?,
    pageSize: Int?,
    cursor: String?,
  ): PaginatedResult<Candidate>
}
