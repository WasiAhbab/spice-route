package com.spiceroute.management;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "branch_assignments")
@IdClass(BranchAssignment.Key.class)
public class BranchAssignment {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "branch_id")
    private UUID branchId;

    protected BranchAssignment() {}

    public BranchAssignment(UUID userId, UUID branchId) {
        this.userId = userId;
        this.branchId = branchId;
    }

    public static class Key implements Serializable {
        private UUID userId;
        private UUID branchId;

        public Key() {}


        public Key(UUID userId, UUID branchId) {
            this.userId = userId;
            this.branchId = branchId;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key key)) return false;
            return Objects.equals(userId, key.userId) && Objects.equals(branchId, key.branchId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, branchId);
        }
    }
}
