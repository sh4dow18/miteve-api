package sh4dow18.miteve_api.entities

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var email: String,
    var password: String,
    @OneToMany(mappedBy = "user", targetEntity = Profile::class)
    var profilesList: List<Profile>,
    @OneToMany(mappedBy = "user", targetEntity = BugReport::class)
    var bugReportsList: List<BugReport>,
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false, referencedColumnName = "id")
    var role: Role
)