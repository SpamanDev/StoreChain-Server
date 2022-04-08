package com.cobnet.spring.boot.entity;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.springframework.data.util.Pair;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Store implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String location;

    private String name;

    private String phone;

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(mappedBy = "store", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<Service> services = new HashSet<>();

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(mappedBy = "store", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<Position> positions = new HashSet<>();

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(mappedBy = "store", orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<Staff> crew = new HashSet<>();

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(mappedBy = "store", cascade = CascadeType.PERSIST)
    private Set<Work> works = new HashSet<>();

    public Store() {}

    public Store(String location, String name, String phone) {

        this(location, name, phone, new HashSet<>(), new HashSet<>());
    }

    public Store(String location, String name, String phone, Set<Service> services, Set<Position> positions, Staff... crew) {
        this.location = location;
        this.name = name;
        this.phone = phone;
        this.services.addAll(services);
        this.positions.addAll(positions);
        this.crew.addAll(Arrays.asList(crew));
    }

    public long getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public Set<Staff> getCrew() {
        return crew;
    }

    public Set<Service> getServices() {
        return services;
    }

    public Set<Position> getPositions() {
        return positions;
    }

    public boolean addStaff(Staff staff) {

        return this.crew.add(staff);
    }

    public boolean addStaff(User user) {

        return this.addStaff(new Staff(this, user, this.getDefaultPosition().get()));
    }

    public boolean addService(Service service) {

        return this.services.add(service);
    }

    public boolean addService(String name) {

        return this.addService(new Service(this, name));
    }

    public boolean addPosition(Position position) {

        return this.positions.add(position);
    }

    public boolean addPosition(String name, boolean isDefault) {

        return this.addPosition(new Position(this, name, isDefault));
    }

    public boolean addPosition(String name) {

        return this.addPosition(name, false);
    }

    public boolean addWork(Work work) {

        return this.works.add(work);
    }

    public boolean addWork(Service service, Staff... workers) {

        return this.addWork(new Work(this, service, workers));
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Optional<Position> getDefaultPosition() {

        return this.getPositions().stream().filter(Position::isDefault).findFirst();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + id + ", " +
                "location = " + location + ", " +
                "name = " + name + ", " +
                "phone = " + phone + ", " +
                "crew = " + crew + ", " +
                "services = " + services + ", " +
                "positions = " + positions + ")";
    }

    public static class Builder {

        private String location;

        private String name;

        private String phone;

        private Set<Object> services = new HashSet<>();

        private Set<Object> positions = new HashSet<>();

        private Set<Object> crew = new HashSet<>();

        public Builder setLocation(String location) {

            this.location = location;

            return this;
        }

        public Builder setName(String name) {

            this.name = name;

            return this;
        }

        public Builder setPhone(String phone) {

            this.phone = phone;

            return this;
        }

        public Builder setServices(Service... services) {

            this.services = Arrays.stream(services).collect(Collectors.toSet());

            return this;
        }

        public Builder setServices(String... services) {

            this.services = Arrays.stream(services).collect(Collectors.toSet());

            return this;
        }

        public Builder setPositions(Position... positions) {

            this.positions = Arrays.stream(positions).collect(Collectors.toSet());

            return this;
        }

        @SafeVarargs
        public final Builder setPositions(Pair<String, Boolean>... positions) {

            this.positions = Arrays.stream(positions).collect(Collectors.toSet());

            return this;
        }

        public Builder setPositions(String... positions) {

            this.positions = Arrays.stream(positions).collect(Collectors.toSet());

            return this;
        }

        public Builder setCrew(Staff... crew) {

            this.crew = Arrays.stream(crew).collect(Collectors.toSet());

            return this;
        }

        @SafeVarargs
        public final Builder setCrew(Pair<User, Position>... crew) {

            this.crew = Arrays.stream(crew).collect(Collectors.toSet());

            return this;
        }

        public Builder setCrew(User... crew) {

            this.crew = Arrays.stream(crew).collect(Collectors.toSet());

            return this;
        }

        public Store build() {

            Store store = new Store(this.location, this.name, this.phone);

            store.services = this.services.stream().map(service -> {

                if(service instanceof String name2) {

                    return new Service(store, name2);
                }

                return (Service) service;

            }).collect(Collectors.toSet());

            store.positions = this.positions.stream().map(position -> {

                if(position instanceof Pair pair1) {

                    return new Position(store, (String) pair1.getFirst(), (Boolean) pair1.getSecond());
                }

                if(position instanceof String name1) {

                    return new Position(store, name1, false);
                }

                return (Position) position;

            }).collect(Collectors.toSet());

            store.crew = this.crew.stream().map(staff -> {

                if (staff instanceof Pair pair) {

                    return new Staff(store, (User) pair.getFirst(), (Position) pair.getSecond());
                }

                if(staff instanceof User user) {

                    return new Staff(store, user, store.getDefaultPosition().get());
                }

                return (Staff)staff;

            }).collect(Collectors.toSet());

            return store;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Store store = (Store) o;
        return Objects.equals(id, store.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
