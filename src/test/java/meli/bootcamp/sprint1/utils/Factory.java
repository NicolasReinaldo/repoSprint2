package meli.bootcamp.sprint1.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import meli.bootcamp.sprint1.dto.response.*;
import meli.bootcamp.sprint1.entity.Category;
import meli.bootcamp.sprint1.entity.Post;
import meli.bootcamp.sprint1.entity.Product;
import meli.bootcamp.sprint1.entity.User;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Factory {

    private final List<User> users;
    private List<Category> categories;

    public Factory() {
        this.categories = this.loadCategories();
        this.users = this.loadUsers();
    }

    private List<Category> loadCategories() {
        File file = null;
        try {
            file = ResourceUtils.getFile("classpath:static/categories.json");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<List<Category>> typeRef = new TypeReference<>() {
        };
        List<Category> categories = null;
        try {
            categories = objectMapper.readValue(file, typeRef);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return categories;
    }

    private List<User> loadUsers() {
        File file = null;
        try {
            file = ResourceUtils.getFile("classpath:static/users.json");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<List<User>> typeRef = new TypeReference<>() {
        };
        List<User> users = null;
        try {
            users = objectMapper.readValue(file, typeRef);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }

    public static LastPostsDto generateLastPostDto(){
        return new LastPostsDto(2,List.of(new PostDto(  1,  1,  LocalDate.now(),
                new ProductDto( 1,
                        "Tele",
                        "Gamer",
                        "Razer",
                        "Red Black",
                        "Special Edition"),1,100.00)));

    }

    public static Post generatePost(){
        Product product = new Product(1,"Tele","Gamer","Razer","Special Edition","Red Black");
        return new Post(product, LocalDate.now(),new Category(1,"Electronic"),100.00);
    }
    public static User generateSeller(){
       return new User(1,"Nico",true, List.of(generatePost()), Collections.emptyList(),List.of(2));
    }

    public static User generateUser(){
        return new User(2,"Marco",false, Collections.emptyList(),List.of(1),Collections.emptyList());
    }
    public static UserWFollowerListDto generateUserDtoList(String order){
        List<UserDto> userDtoList =  new ArrayList<UserDto>();
        userDtoList.add(new UserDto(3,"Fatima Noble"));
        userDtoList.add(new UserDto(2,"Brenda Torrico"));
        userDtoList.add(new UserDto(1,"Ailen Pereira"));

        return new UserWFollowerListDto(4,"Geronimo Schmidt",orderList(userDtoList,order));
    }

    private static List<UserDto> orderList(List<UserDto> userDtoList, String order) {
        if(order == "name_asc"){
            return userDtoList = userDtoList.stream()
                    .sorted(Comparator.comparing(UserDto::getUser_name))
                    .toList();
        } else if(order == "name_desc"){
            return userDtoList = userDtoList.stream()
                    .sorted(Comparator.comparing(UserDto::getUser_name).reversed())
                    .toList();
        }else {
            return userDtoList;
        }
    }

    public static List<User> generateUsersList(){
        return List.of(new User(1,"Ailen Pereira",true, Collections.emptyList(),Collections.emptyList(),Collections.emptyList())
                ,new User(2,"Brenda Torrico",true, Collections.emptyList(),Collections.emptyList(),Collections.emptyList())
                ,new User(3,"Fatima Noble",true, Collections.emptyList(),Collections.emptyList(),Collections.emptyList())
                ,new User(4,"Geronimo Schmidt",true, Collections.emptyList(),Collections.emptyList(),List.of(1,2,3)));
    }
}
