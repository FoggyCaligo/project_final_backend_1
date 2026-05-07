package com.today.fridge.bookmark.service;

import com.today.fridge.bookmark.dto.BookmarkedRecipeResponse;
import com.today.fridge.bookmark.entity.Bookmark;
import com.today.fridge.bookmark.repository.BookmarkRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

    @Test
    @DisplayName("사용자의 북마크 목록을 조회하면 Repository가 반환한 레시피 DTO 목록을 그대로 반환한다")
    void getBookmarkedRecipes_returnsBookmarkedRecipeResponses() {
        // given
        Long userId = 1L;
        List<BookmarkedRecipeResponse> expected = List.of(
                new BookmarkedRecipeResponse(101L, "김치찌개"),
                new BookmarkedRecipeResponse(102L, "된장찌개")
        );
        when(bookmarkRepository.findBookmarkedRecipesByUserId(userId)).thenReturn(expected);

        // when
        List<BookmarkedRecipeResponse> actual = bookmarkService.getBookmarkedRecipes(userId);

        // then
        assertThat(actual).hasSize(2);
        assertThat(actual)
                .extracting(BookmarkedRecipeResponse::getRecipeName)
                .containsExactly("김치찌개", "된장찌개");
        verify(bookmarkRepository).findBookmarkedRecipesByUserId(userId);
    }

    @Test
    @DisplayName("북마크가 없던 레시피를 등록하면 User와 Recipe를 조회한 뒤 Bookmark를 저장한다")
    void addBookmark_savesBookmarkWhenNotExists() {
        // given
        Long userId = 1L;
        Long recipeId = 10L;
        User user = User.create("tester", "tester@example.com", "encoded-password", "테스터");
        Recipe recipe = Recipe.builder()
                .title("감자조림")
                .isActive(true)
                .build();

        when(bookmarkRepository.existsByUser_UserIdAndRecipe_RecipeId(userId, recipeId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        // when
        bookmarkService.addBookmark(userId, recipeId);

        // then
        ArgumentCaptor<Bookmark> bookmarkCaptor = ArgumentCaptor.forClass(Bookmark.class);
        verify(bookmarkRepository).save(bookmarkCaptor.capture());

        Bookmark savedBookmark = bookmarkCaptor.getValue();
        assertAll(
                () -> assertThat(savedBookmark.getUser()).isSameAs(user),
                () -> assertThat(savedBookmark.getRecipe()).isSameAs(recipe),
                () -> assertThat(savedBookmark.getCreatedAt()).isNotNull()
        );
    }

    @Test
    @DisplayName("이미 등록된 북마크를 다시 등록하려고 하면 추가 저장하지 않는다")
    void addBookmark_doesNotSaveWhenAlreadyExists() {
        // given
        Long userId = 1L;
        Long recipeId = 10L;
        when(bookmarkRepository.existsByUser_UserIdAndRecipe_RecipeId(userId, recipeId)).thenReturn(true);

        // when
        bookmarkService.addBookmark(userId, recipeId);

        // then
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
        verifyNoInteractions(userRepository, recipeRepository);
    }

    @Test
    @DisplayName("북마크 삭제를 요청하면 userId와 recipeId 기준으로 삭제 Repository 메서드를 호출한다")
    void removeBookmark_deletesByUserIdAndRecipeId() {
        // given
        Long userId = 1L;
        Long recipeId = 10L;

        // when
        bookmarkService.removeBookmark(userId, recipeId);

        // then
        verify(bookmarkRepository).deleteByUser_UserIdAndRecipe_RecipeId(userId, recipeId);
    }

    @Test
    @DisplayName("북마크 상태 확인은 Repository의 존재 여부 결과를 그대로 반환한다")
    void checkBookmarkStatus_returnsExistsResult() {
        // given
        Long userId = 1L;
        Long recipeId = 10L;
        when(bookmarkRepository.existsByUser_UserIdAndRecipe_RecipeId(userId, recipeId)).thenReturn(true);

        // when
        boolean result = bookmarkService.checkBookmarkStatus(userId, recipeId);

        // then
        assertThat(result).isTrue();
        verify(bookmarkRepository).existsByUser_UserIdAndRecipe_RecipeId(userId, recipeId);
    }
}
