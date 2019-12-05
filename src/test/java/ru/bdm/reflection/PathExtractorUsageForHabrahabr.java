package ru.bdm.reflection;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static ru.bdm.reflection.PathExtractor.getPath;

/**
 * User: D.Brusentsov
 * Date: 22.04.13
 * Time: 20:21
 */
public class PathExtractorUsageForHabrahabr {

    public static class Pet {
        private String name;
        private Human owner;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Human getOwner() {
            return owner;
        }

        public void setOwner(Human owner) {
            this.owner = owner;
        }
    }

    public static class Human {

        private String name;
        private Date birth;
        private List<Human> relatives;

        //getters and setters omitted

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getBirth() {
            return birth;
        }

        public void setBirth(Date birth) {
            this.birth = birth;
        }

        public List<Human> getRelatives() {
            return relatives;
        }

        public void setRelatives(List<Human> relatives) {
            this.relatives = relatives;
        }
    }

    @Test
    public void getPetName() {
        String name = getPath(Pet.class, Pet::getName);

        Assert.assertEquals("name", name);
    }

    @Test
    public void getPetOwnerName() {
        String ownerName = getPath(Pet.class, pet -> {
            pet.getOwner().getName();
        });

        Assert.assertEquals("owner.name", ownerName);
    }

    @Test
    public void getPetOwnerRelativesBirth() {
        String ownerRelativesBirth = getPath(Pet.class, pet -> {
            PathExtractor.mask(pet.getOwner().getRelatives()).getBirth();
        });

        Assert.assertEquals("owner.relatives.birth", ownerRelativesBirth);
    }
}
