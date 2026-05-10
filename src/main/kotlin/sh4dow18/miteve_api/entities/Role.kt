package sh4dow18.miteve_api.entities

import jakarta.persistence.*

@Entity
@Table(name = "roles")
class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @OneToMany(mappedBy = "role", targetEntity = User::class)
    var usersList: List<User>,
    @ManyToMany(targetEntity = Privilege::class)
    @JoinTable(
        name = "roles_privileges",
        joinColumns = [JoinColumn(name = "role_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "privilege_id", referencedColumnName = "id")]
    )
    var privilegesList: Set<Privilege>
) {
    override fun equals(other: Any?): Boolean {
        // Check if the current object is the same instance as other
        if (this === other) return true
        // Check if other is a Role
        if (other !is Role) return false
        // Compare the id of this object with the id of the other object
        return id == other.id
    }
    // Use the hashCode of the "id" field as the hash code for the entire object
    override fun hashCode(): Int = id.hashCode()
}