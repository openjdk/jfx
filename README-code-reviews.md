Code Review Policies
====================

Project Stewardship
-------------------

The OpenJFX Project is guided by the Project Leads and Reviewers.

__Project Co-Lead__: Kevin Rushforth (OpenJDK ID: `kcr`; GitHub ID: `kevinrushforth`) <br>
__Project Co-Lead__: Johan Vos (OpenJDK ID: `jvos`; GitHub ID: `johanvos`)

### Reviewers

The [List of Reviewers](https://openjdk.org/census#openjfx) is on the OpenJDK Census.

Overview
--------

All code must be reviewed before being pushed to the repository. The short version of the OpenJFX code review policy is:

* We define a formal "Reviewer" role, similar to the JDK project, and outline the responsibilities of Reviewers
* Reviewers, PR authors, and sponsoring Committers verify the following before integration:
    * All feedback from reviewers is addressed
    * All Reviewers who have asked for the chance to review have done so (or indicated that they no longer need to)
    * Sufficient time (at least 1 business day) has passed to allow others to comment
* The code review policies recognize the following different types of changes, with different minimum thresholds for review:
    * Simple, low-impact fixes: 1 reviewer
    * Higher-impact fixes: 2 reviewers
    * Features / API changes: CSR for approving the change, including approval by a "lead"; implementation then needs 2 reviewers for the code (as with other "higher-impact" fixes above)

Details
-------

Code reviews are important to maintain high-quality contributions, but we recognize that not every type of change needs the same level of review. Without lowering our standards of quality, we want to make it easier to get low-impact changes (simple bug fixes) accepted.

In support of this, the following review policies are in effect. Many of these will involve judgment calls, especially when it comes to deciding whether a fix is low impact vs. high-impact, and that's OK. It doesn't have to be perfect.

### 1. The Reviewer role for the OpenJFX Project

We define a formal "Reviewer" role, similar to the JDK project. A [Reviewer](https://openjdk.org/census#openjfx) is responsible for reviewing code changes and helping to determine whether a change is suitable for including into OpenJFX. We expect Reviewers to feel responsible not just for their piece, but for the quality of the JavaFX library as a whole. In other words, the role of Reviewer is one of stewardship. See the following section for what constitutes a good review.

An experienced Committer can eventually become a Reviewer by providing good quality fixes and participating in code reviews over time, demonstrating the high-level of skill and understanding needed to be a competent reviewer. The JDK uses a threshold of 32 significant contributions. Without wanting to relax this standard too much, one thing we may consider is that a Committer with, say, 24 commits, who regularly participates in reviews, offering good feedback, might be just as good a reviewer (or maybe even better) as someone with 32 commits who rarely, if ever, provides feedback on proposed bug fixes. This is meant to be a somewhat loose guideline. It is up to the Reviewers and the Project Leads to decide whether and when a new Committer is ready to become a Reviewer.

### 2. Code review policies

All code reviews must be done via a pull request submitted against this GitHub repo, [openjdk/jfx](https://github.com/openjdk/jfx). A JBS bug ID must exist before the pull request will be reviewed. See [CONTRIBUTING.md](CONTRIBUTING.md) for information on how to submit a pull request.

All fixes must be reviewed by at least one reviewer with the "Reviewer" role (aka a "R"eviewer). We have a different code review threshold for different types of changes. If there is disagreement as to whether a fix is low-impact or high-impact, then it is considered high-impact. In other words we will always err on the side of quality by "rounding up" to the next higher category. The contributor can say whether they think something is low-impact or high-impact, but it is up to a Reviewer to confirm this. A Reviewer either adds a comment indicating that they think a single review is sufficient, or else issues the Skara `/reviewers 2` command requesting a second reviewer (a Reviewer can request more than 2 reviewers in some cases where a fix might be especially risky or cut across multiple functional areas).

Review comments can either be added directly to the GitHub pull request, or by replying to the auto-generated "RFR" (Request For Review) email thread. The Skara bot will cross-forward between them. To approve a pull request, a reviewer must do that in the PR itself. See the following [GitHub help page](https://help.github.com/en/articles/reviewing-proposed-changes-in-a-pull-request) for help on reviewing a pull request.

#### Guidelines for reviewing a PR:

By default, a PR is marked as ready once any "R"eviewer reviews and approves it. Because of this, those who have the Reviewer role should do the following when reviewing a PR _before_ approving it:

* Determine whether this needs 2 reviewers and whether it needs a CSR; issue the `/reviewers 2` or `/csr` command as needed (note that `/reviewers 2` requires approval from 2 total reviewers, at least one of which has the Reviewer role; if you really feel that a review from a second "R"eviewer is needed, use the command `/reviewers 2 reviewers`)
    * If you want to indicate your approval, but still feel additional reviewers are needed, you may increase the number of reviewers (e.g., from 2 to 3)
    * If you want an area expert to review a PR, indicate this in a comment of the form: `Reviewers: @PERSON1 @PERSON2`; the requested reviewers can indicate whether or not they plan to review it
    * If you want to ensure that you have the opportunity to review this PR yourself, add a comment of the form: `@PRAUTHOR Wait for me to review this PR`, optionally add any concerns you might have
* Check that the PR target branch is correct
    * An ordinary (non-backport) PR must target the `master` branch in almost all cases
    * A backport PR (which will have the `backport` label) must target the current stabilization branch; a Reviewer should check that the bug being fixed meets the criteria for the current phase of stabilization

Here is a list of things to keep in mind when reviewing a PR. This applies to anyone doing a review, but especially a "R"eviewer:

* Make sure you understand why there was an issue to begin with, and why/how the proposed PR solves the issue
* Carefully consider the risk of regression
* Carefully consider any compatibility concerns
* Check whether it adds, removes, or modifies any public or protected API, even implicitly (such as a public method that overrides a protected method, or a class that is moved from a non-exported to an exported package); if it does, indicate that it needs a CSR
* Focus first on substantive comments rather than stylistic comments
* Check whether there is an automated test; if not, ask for one, if it is feasible
* Make sure that the PR has executed the GitHub Actions (GHA) tests; if they aren't being run, ask the PR author to enable GHA workflows; if the test fails on some platforms, check whether it is a real bug (sometimes a job fails because of GHA infrastructure changes or we see a spurious GHA failure)
* Test the code locally if you have any concerns as to whether and how it works; as a helpful tip, merge the latest upstream master into your locally fetch PR review branch before testing
* If the PR source branch hasn't synced up from master in a long time, or if there is an upstream commit not in the source branch that might interfere with the PR, consider asking the PR author to merge the latest upstream master, so we will get an up-to-date GHA run

#### Before you integrate or sponsor a PR:

Skara will mark a PR as "ready" once the minimum number of reviewers have reviewed it. Before you integrate or sponsor the PR, ensure that the following have been done:

* All substantive feedback has been addressed, especially any objections from one with a Reviewer role
    * If you have pushed any changes in response to a Reviewer's substantive comments, wait for them to re-review the latest version of your PR with those changes (to ensure they are satisfied with the way you addressed them)
* All Reviewers who have requested the chance to review have done so (or indicated that they are OK with it going in without their review). In rare cases a Project Lead may override this
* The PR has been "rfr" (as indicated by Skara) for at least 1 business day (at least 24 hours, not including weekends or major holidays). This is to allow sufficient time for those reviewers who might be in other time zones the chance to review if they have concerns. This is measured from the time that Skara has most recently added the "rfr" label (for example, for a PR that was previously in Draft mode, wait for at least 1 business day after the PR has been taken out of Draft and marked "rfr"). In rare cases (e.g., a build breakage) a Reviewer might give the OK to integrate without waiting for 1 business day.

#### A. Low-impact bug fixes.

These are typically isolated bug fixes with little or no impact beyond fixing the bug in question; included in this category are test fixes (including most new tests), doc fixes, and fixes to sample applications (including most new samples).

One (1) "R"eviewer is usually sufficient to accept such changes.

#### B. Higher impact bug fixes or RFEs.

These include changes to the implementation that potentially have a performance or behavioral impact, or are otherwise broad in scope. Some larger bug fixes will fall into this category, as will any fixes in high-risk areas (e.g., CSS).

At least two (2) reviewers must approve to accept such changes, at least one of whom must be a "R"eviewer.

#### C. New features / API additions.

This includes behavioral changes, additions to the FXML or CSS spec, etc.

Feature requests come with a responsibility beyond just saying "here is the code for this cool new feature, please take it". There are many factors to consider for even small features. Larger features will need a significant contribution in terms of API design, coding, testing, maintainability, etc.

A feature should be discussed up-front on the openjfx-dev mailing list to get early feedback on the concept (is it a feature we are likely to accept) and the direction the API and/or implementation should take.

To ensure that new features are consistent with the rest of the API and the desired direction of the Project, a CSR is required for a new Feature, API addition, or behavioral change. The CSR must be reviewed and approved by a "lead" of the Project. Currently this is either Kevin Rushforth or Johan Vos as indicated above.

The review of the implementation follows the same "two reviewer" standard for higher-impact changes as described in category B. The two code reviewers for the implementation may or may not include the Lead who reviewed the CSR. The review / approval of the CSR is an independent step from the review / approval of the code change, although they can proceed in parallel.
