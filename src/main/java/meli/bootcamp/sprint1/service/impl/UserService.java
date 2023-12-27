package meli.bootcamp.sprint1.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

import meli.bootcamp.sprint1.dto.response.FollowersDto;
import meli.bootcamp.sprint1.dto.response.FollowedDto;
import meli.bootcamp.sprint1.dto.response.UserFollowedDto;
import meli.bootcamp.sprint1.exception.EmptyListException;
import meli.bootcamp.sprint1.dto.response.LastPostsDto;
import meli.bootcamp.sprint1.dto.response.PostDto;
import meli.bootcamp.sprint1.dto.response.ProductDto;

import meli.bootcamp.sprint1.dto.response.FollowerDto;
import meli.bootcamp.sprint1.dto.response.UserDto;

import org.springframework.stereotype.Service;

import meli.bootcamp.sprint1.dto.request.NewPostDto;
import meli.bootcamp.sprint1.dto.response.BaseResponseDto;
import meli.bootcamp.sprint1.entity.Category;
import meli.bootcamp.sprint1.entity.Post;
import meli.bootcamp.sprint1.entity.Product;
import meli.bootcamp.sprint1.entity.User;
import meli.bootcamp.sprint1.exception.BadRequestException;
import meli.bootcamp.sprint1.repository.IGeneralRepository;
import meli.bootcamp.sprint1.repository.impl.GeneralRepository;
import meli.bootcamp.sprint1.service.IUserService;
import meli.bootcamp.sprint1.util.Mapper;

import static meli.bootcamp.sprint1.util.Validator.*;

@Service
public class UserService implements IUserService {
  private final IGeneralRepository repository;

  public UserService(GeneralRepository repository) {
    this.repository = repository;
  }

  @Override
  public BaseResponseDto addPost(NewPostDto newPost) {
    User user = getUserFromRepository(newPost.getUser_id());
    verifyIfUserExists(user);

    Product newProductEntity = Mapper.map(newPost.getProduct(), Product.class);

    Category category = this.repository.findCategoryById(newPost.getCategory());
    if (category == null) {
      throw new BadRequestException("Category not found");
    }

    Post newPostEntity = new Post(newProductEntity, newPost.getDate(), category, newPost.getPrice());

    user.addPost(newPostEntity);

    return new BaseResponseDto("Post added");
  }

  @Override
  public BaseResponseDto followUser(int userId, int userIdToFollow) {
    User user = getUserFromRepository(userId);
    User userToFollow = getUserFromRepository(userIdToFollow);

    verifyIfUserExists(user);
    verifyIfUserExists(userToFollow);

    validateIsSeller(userToFollow);

    validateEqualsId(userId, userIdToFollow);

    Optional<Integer> userFollowedId = user.getFollowed().stream().filter(id -> id == userIdToFollow).findFirst();

    if (userFollowedId.isPresent())
      return new BaseResponseDto("The user is already followed");

    repository.addNewFollowed(userId, userIdToFollow);

    return new BaseResponseDto("User followed");
  }

  @Override
  public UserDto getFollowersById(int id, String order) {
    User user = getUserFromRepository(id);
    verifyIfUserExists(user);

    List<Integer> followers = Optional.ofNullable(user.getFollowers())
            .filter(follower -> !follower.isEmpty())
            .orElseThrow(() -> new EmptyListException("The User " + id + " has no followers users"));

    List<FollowerDto> followerDtoList = followers.stream()
            .map(this::getUserFromRepository)
            .map(findFollower -> new FollowerDto(findFollower.getId(), findFollower.getName()))
            .toList();

    followerDtoList = getOrderFollowerDtos(order, followerDtoList);

    return new UserDto(user.getId(), user.getName(), followerDtoList);
  }

  @Override
  public BaseResponseDto unfollowUser(int userId, int userIdToUnfollow) {
    User userFollower = getUserFromRepository(userId);
    User userFollowed = getUserFromRepository(userIdToUnfollow);

    verifyIfUserExists(userFollower);
    verifyIfUserExists(userFollowed);
    validateEqualsId(userId, userIdToUnfollow);
    validateIsSeller(userFollowed);

    if (userFollower.getFollowed().isEmpty() && userFollowed.getFollowed().isEmpty()) {
      throw new BadRequestException("Followed list and following lists are empty");
    }

    boolean unfollowed = this.repository.unfollowUser(userId, userIdToUnfollow);

    if (unfollowed) {
      return new BaseResponseDto("User " + userIdToUnfollow + " was unfollowed");
    } else {
      return new BaseResponseDto("Error unfollowing user, user " + userId + " doesn't follow user " + userIdToUnfollow);
    }
  }

  @Override
  public FollowersDto getFollowersByUserId(int userId) {
    User user = getUserFromRepository(userId);
    verifyIfUserExists(user);
    return new FollowersDto(user.getId(), user.getName(), user.getFollowers().size());
  }

  @Override
  public UserFollowedDto getFollowed(Integer id, String order) {
    User user = getUserFromRepository(id);

    verifyIfUserExists(user);

    if (user.getFollowed().isEmpty()) {
      throw new EmptyListException("The User " + id + " has no followed users");
    }

    List<FollowedDto> followed = user.getFollowed().stream()
            .map(u -> new FollowedDto(u, getUserFromRepository(u).getName()))
            .toList();

    followed = getOrderFollowedDtos(order, followed);

    return new UserFollowedDto(user.getId(), user.getName(), followed);
  }

  @Override
  public LastPostsDto getLastPostsFromFollowed(int userId) {
    User user = getUserFromRepository(userId);
    verifyIfUserExists(user);
    List<Integer> followedId = user.getFollowed();
    List<User> followed = followedId.stream().map(this::getUserFromRepository).toList();
    List<PostDto> postsDto = new ArrayList<>();
    for (User follow : followed) {
      List<PostDto> postsToAdd = follow.getPosts().stream()
              .filter(post -> isFromLastTwoWeeks(post.getDate()))
              .map(post -> {
                Product product = post.getProduct();
                ProductDto productDto = new ProductDto(product.getId(), product.getName(), product.getType(),
                        product.getBrand(), product.getColor(), product.getNotes());
                return new PostDto(follow.getId(), post.getId(), post.getDate(), productDto, post.getCategory().getId(),
                        post.getPrice());
              })
              .toList();
      postsDto.addAll(postsToAdd);
    }
    return new LastPostsDto(userId, postsDto);
  }

  private boolean isFromLastTwoWeeks(LocalDate date) {
    return date.isAfter(LocalDate.now().minusWeeks(2)) ||
            date.equals(LocalDate.now().minusWeeks(2));
  }

  @Override
  public LastPostsDto getLastPostsOrdered(int userId, String order) {
    LastPostsDto lastPostsDto = getLastPostsFromFollowed(userId);
    List<PostDto> postsDto = lastPostsDto.getPosts();

    if ("date_asc".equals(order)) {
      postsDto.sort(Comparator.comparing(PostDto::getDate));
    } else if ("date_desc".equals(order)) {
      postsDto.sort(Comparator.comparing(PostDto::getDate).reversed());
    }

    return new LastPostsDto(userId, postsDto);
  }

  @Override
  public LastPostsDto getLastPosts(int userId, String order) {
    if ("date_asc".equals(order) || "date_desc".equals(order)) {
      return getLastPostsOrdered(userId, order);
    } else {
      return getLastPostsFromFollowed(userId);
    }
  }

  private User getUserFromRepository(int id) {
    return this.repository.findUserById(id);
  }

  private List<FollowedDto> getOrderFollowedDtos(String order, List<FollowedDto> followed) {
    if (order == null || order.equals("name_asc")) {
      followed = followed.stream()
              .sorted(Comparator.comparing(FollowedDto::getUser_name))
              .toList();
    } else if (order.equals("name_desc")) {
      followed = followed.stream()
              .sorted(Comparator.comparing(FollowedDto::getUser_name).reversed())
              .toList();
    }
    else{
      throw  new BadRequestException("Parameter " + order + " is not valid");
    }
    return followed;
  }

  private List<FollowerDto> getOrderFollowerDtos(String order, List<FollowerDto> followerDtoList) {
    if (order == null || order.equals("name_asc")) {
      followerDtoList = followerDtoList.stream()
              .sorted(Comparator.comparing(FollowerDto::getUser_name))
              .toList();
    } else if (order.equals("name_desc")) {
      followerDtoList = followerDtoList.stream()
              .sorted(Comparator.comparing(FollowerDto::getUser_name).reversed())
              .toList();
    } else {
      throw  new BadRequestException("Parameter " + order + " is not valid");
    }
      return followerDtoList;
  }

}