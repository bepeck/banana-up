package ru.bdm.reflection;

import org.junit.Test;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static junit.framework.Assert.assertEquals;
import static ru.bdm.reflection.PathExtractorJava8.path;
import static ru.bdm.reflection.PathExtractorJava8.start;

/**
 * User: D.Brusentsov
 * Date: 22.04.13
 * Time: 20:21
 */
public class PathExtractorJava8UsageForHabrahabr {

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
        String name = path(Pet.class, Pet::getName);

        assertEquals("name", name);

        name = path(Pet.class, (t) -> t.getName());

        assertEquals("name", name);

        name = path(Pet.class, new Consumer<Pet>() {
            @Override
            public void accept(Pet pet) {
                pet.getName();
            }
        });

        assertEquals("name", name);
    }

    @Test
    public void getPetOwnerName() {
        String ownerName = start(Pet.class, Pet::getOwner).then(Human::getName).end();

        assertEquals("owner.name", ownerName);

        ownerName = path(Pet.class, pet -> pet.getOwner().getName());

        assertEquals("owner.name", ownerName);

        ownerName = path(Pet.class, new Consumer<Pet>() {
            @Override
            public void accept(Pet pet) {
                pet.getOwner().getName();
            }
        });

        assertEquals("owner.name", ownerName);
    }

    @Test
    public void getPetOwnerRelativesBirth() {
        String ownerRelativesBirth = start(Pet.class, Pet::getOwner)
                .thenMask(Human::getRelatives)
                .end(Human::getBirth);

        assertEquals("owner.relatives.birth", ownerRelativesBirth);

        ownerRelativesBirth = start(Pet.class, pet -> pet.getOwner())
                .thenMask(human -> human.getRelatives())
                .end(human -> human.getBirth());

        assertEquals("owner.relatives.birth", ownerRelativesBirth);

        ownerRelativesBirth = start(Pet.class, new Function<Pet, Human>() {
            @Override
            public Human apply(Pet pet) {
                return pet.getOwner();
            }
        }).thenMask(new Function<Human, Collection<Human>>() {
            @Override
            public Collection<Human> apply(Human human) {
                return human.getRelatives();
            }
        }).end(new Consumer<Human>() {
            @Override
            public void accept(Human human) {
                human.getBirth();
            }
        });

        assertEquals("owner.relatives.birth", ownerRelativesBirth);
    }
}
