<h1>Social Responsability Portal </h1>
<h2>⚡ Quick Overview </h2>
<p>The community-centric portal streamlines issue reporting by allowing users to register directly on the platform. After submitting essential details such as name, email, location coordinates, and preferences, users gain access to a range of features, including issue tracking, voting, commenting, and reporting. This approach promotes active community participation and information exchange for efficient issue resolution.</p>
<p><b>Tech Stack: Spring Boot, Spring Data JPA, Spring Security, Java 17, JUnit and Mockito, Docker</b></p>
<h2>🚀 Features</h2>
<ol>
  <b>
    <li>Registration and Login:</li>
  </b>
  <ul>
    <li>User registration with essential details: fullname, email, location (geographical coordinates), radius of interest, age, gender, and optional profile picture.</li>
  </ul>
  <br>
  <b>
    <li>Visitor Browsing:</li>
  </b>
  <ul>
    <li>View current issues on a map within the user's region of interest.</li>
  </ul>
  <br>
  <b>
    <li>Issue Management:</li>
  </b>
  <ul>
    <li>Create and manage issues with details: title, short description, relevant picture(s), and physical location.</li>
    <li>Confirm or deny the existence of an issue using a thumbs-up and thumbs-down system.</li>
    <li>Upvote and downvote issues to determine their significance.</li>
    <li>Report new issues and track their status.</li>
  </ul>
  <br>
  <b>
    <li>Comments and Feedback:</li>
  </b>
  <ul>
    <li>Provide feedback and comments on issues.</li>
    <li>Edit or delete one's own comments.</li>
    <li>Issues with twice as many downvotes as upvotes are archived automatically.</li>
  </ul>
  <br>
  <b>
    <li>Statistics and Monitoring:</li>
  </b>
  <ul>
    <li>Admin overview of active and archived issues.</li>
    <li>Delete inappropriate comments.</li>
    <li>Archive issues as needed.</li>
    <li>Promote users to admin status.</li>
  </ul>
</ol>
<h2>✍️ API Endpoints</h2>
<ol>
  <b>
    <li>/api/v1/user</li>
  </b>
      <details>
        <table>
          <tr>
            <th>Path</th>
            <th>Method</th>
            <th>QueryParam</th>
            <th>Description</th>
          </tr>
          <tr>
            <td>/</td>
            <td>POST</td>
            <td>-</td>
            <td>User registration with optional profile image</td>
          </tr>
          <tr>
            <td>/login</td>
            <td>POST</td>
            <td>-</td>
            <td>User login</td>
          </tr>
          <tr>
            <td>/profile-pic</td>
            <td>GET</td>
            <td>id</td>
            <td>Get user's profile picture by ID</td>
          </tr>
        </table>
      </details>
      <li>/api/v1/main</li>
      <details>
        <table>
          <tr>
            <th>Path</th>
            <th>Method</th>
            <th>QueryParam</th>
            <th>Description</th>
          </tr>
          <tr>
            <td>/issues</td>
            <td>POST</td>
            <td>-</td>
            <td>Add a new issue</td>
          </tr>
          <tr>
            <td>/issues</td>
            <td>GET</td>
            <td>pageNo, noOfItems</td>
            <td>List issues</td>
          </tr>
          <tr>
            <td>/issues/{issueId}/comments</td>
            <td>GET</td>
            <td>pageNo, itemsPerPage</td>
            <td>Get comments by issue ID</td>
          </tr>
          <tr>
            <td>/issues/{issueId}/comments</td>
            <td>POST</td>
            <td>-</td>
            <td>Add a comment to an issue</td>
          </tr>
          <tr>
            <td>/issues/{issueId}/vote</td>
            <td>POST</td>
            <td>voteValue</td>
            <td>Vote on an issue</td>
          </tr>
          <tr>
            <td>/comments/{commentId}</td>
            <td>DELETE</td>
            <td>-</td>
            <td>Delete a comment</td>
          </tr>
        </table>
      </details>
      <li>/api/v1/admin</li>
      <details>
        <table>
          <tr>
            <th>Path</th>
            <th>Method</th>
            <th>QueryParam</th>
            <th>Description</th>
          </tr>
          <tr>
            <td>/issues/{pageNo}</td>
            <td>GET</td>
            <td>status, pageSize</td>
            <td>Get issues by page number</td>
          </tr>
          <tr>
            <td>/issues/{issueId}/archive</td>
            <td>PUT</td>
            <td>-</td>
            <td>Archive an issue</td>
          </tr>
        </table>
      </details>
</ol>
<h2>🏗️ Architecture</h2>

<p><b>N Tier Architecture:</b></p>
<i>Client <--> Controller <--> Service <--> DAO <--> DB</i>

## System Architecture Diagram

```mermaid
graph TB
    subgraph Client["👤 Client Layer"]
        Web["🌐 Web Browser<br/>REST Client"]
    end

    subgraph Security["🔒 Security Layer"]
        JwtFilter["JWT Token Filter"]
        AuthProvider["Authentication<br/>Provider"]
    end

    subgraph API["🔌 REST API Layer"]
        AdminCtrl["⚙️ AdminController<br/>/api/v1/admin"]
        IssueCtrl["📋 IssueController<br/>/api/v1/issues"]
        UserCtrl["👥 UserController<br/>/api/v1/users"]
    end

    subgraph Services["🧠 Business Logic Layer"]
        subgraph AdminSvc["Admin Operations"]
            AdminService["AdministrationService"]
        end
        subgraph ContentSvc["Content Services"]
            IssueService["IssueService"]
            CommentService["CommentService"]
            VoteService["VoteService"]
        end
        subgraph UserSvc["User Services"]
            UserService["UserService"]
            ImageService["ImageService"]
        end
    end

    subgraph Models["📦 Domain Model Layer"]
        User["User"]
        Issue["Issue"]
        Comment["Comment"]
        Vote["Vote"]
        UserImage["UserImage"]
        IssueImage["IssueImage"]
        Role["Role"]
        Location["Location"]
    end

    subgraph Repositories["🗄️ Data Access Layer"]
        UserRepo["UserEntityRepository"]
        IssueRepo["IssueRepository"]
        CommentRepo["CommentRepository"]
        VoteRepo["VoteRepository"]
        UserImgRepo["UserImageRepository"]
        IssueImgRepo["IssueImageRepository"]
        RoleRepo["RoleRepository"]
        LocationRepo["LocationRepository"]
    end

    subgraph Database["💾 Persistence Layer"]
        MariaDB["🗃️ MariaDB<br/>Relational Database"]
        Storage["📁 File Storage<br/>Images & Media"]
    end

    Web -->|HTTP Requests| JwtFilter
    JwtFilter -->|Token Validation| AuthProvider
    AuthProvider -->|Route to Controller| AdminCtrl
    AuthProvider -->|Route to Controller| IssueCtrl
    AuthProvider -->|Route to Controller| UserCtrl

    AdminCtrl -->|Delegate| AdminService
    IssueCtrl -->|Delegate| IssueService
    IssueCtrl -->|Delegate| CommentService
    IssueCtrl -->|Delegate| VoteService
    UserCtrl -->|Delegate| UserService

    AdminService -->|Query| IssueService
    AdminService -->|Manage| UserService
    
    IssueService -->|Handle Media| ImageService
    UserService -->|Handle Media| ImageService

    AdminService -->|Query| IssueRepo
    AdminService -->|Query| UserRepo
    IssueService -->|Query| IssueRepo
    CommentService -->|Query| CommentRepo
    VoteService -->|Query| VoteRepo
    UserService -->|Query| UserRepo
    UserService -->|Query| RoleRepo
    ImageService -->|Query| UserImgRepo
    ImageService -->|Query| IssueImgRepo

    IssueRepo -->|ORM| Issue
    CommentRepo -->|ORM| Comment
    VoteRepo -->|ORM| Vote
    UserRepo -->|ORM| User
    UserImgRepo -->|ORM| UserImage
    IssueImgRepo -->|ORM| IssueImage
    RoleRepo -->|ORM| Role
    LocationRepo -->|ORM| Location

    IssueRepo -->|Persist| MariaDB
    CommentRepo -->|Persist| MariaDB
    VoteRepo -->|Persist| MariaDB
    UserRepo -->|Persist| MariaDB
    UserImgRepo -->|Persist| MariaDB
    IssueImgRepo -->|Persist| MariaDB
    RoleRepo -->|Persist| MariaDB
    LocationRepo -->|Persist| MariaDB

    ImageService -->|Store/Retrieve| Storage

    classDef clientStyle fill:#e3f2fd,stroke:#1976d2,stroke-width:2px,color:#000
    classDef securityStyle fill:#fff3e0,stroke:#f57c00,stroke-width:2px,color:#000
    classDef apiStyle fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#000
    classDef serviceStyle fill:#e8f5e9,stroke:#388e3c,stroke-width:2px,color:#000
    classDef modelStyle fill:#fce4ec,stroke:#c2185b,stroke-width:2px,color:#000
    classDef repoStyle fill:#e0f2f1,stroke:#00796b,stroke-width:2px,color:#000
    classDef dbStyle fill:#ffebee,stroke:#d32f2f,stroke-width:2px,color:#000

    class Web clientStyle
    class JwtFilter,AuthProvider securityStyle
    class AdminCtrl,IssueCtrl,UserCtrl apiStyle
    class AdminService,IssueService,CommentService,VoteService,UserService,ImageService serviceStyle
    class User,Issue,Comment,Vote,UserImage,IssueImage,Role,Location modelStyle
    class UserRepo,IssueRepo,CommentRepo,VoteRepo,UserImgRepo,IssueImgRepo,RoleRepo,LocationRepo repoStyle
    class MariaDB,Storage dbStyle
```

<details>
<ol>
 <li>Controller</li>
  - Keeps all spring REST controllers
  - Define end points
 <li>Service</li>
  - All service classes that hold business logic
 <li>DAO</li>
  - Repository layer
  - Keep all spring JPA data repository
  - Communicates with database  
</ol>
</details>

<h2>🪲 Tests Report</h2>

Tech stack: JUnit 5, Mockito
Approach: Arrange -> Act -> Assert (AAA)
Test were written for the service layer. No integration tests for the moment.

<b>Coverage report -> generated from IntellIJ by running tests with Coverage.</b>
<img width="2102" height="818" alt="image" src="https://github.com/user-attachments/assets/461194a8-44a8-417a-84bb-c038fbf7470b" />


<b>Mutation tests report -> generated by using Maven pitest plugin </b>
<img width="2066" height="788" alt="image" src="https://github.com/user-attachments/assets/18f5ea42-df4e-45d8-85ba-6b0227b1f073" />


AI was used for generating diagrams, some small parts of code (methods names, refactoring, best practices). AI was also used in order to improve IssueServiceTest
quality after running Mutation Tests.




