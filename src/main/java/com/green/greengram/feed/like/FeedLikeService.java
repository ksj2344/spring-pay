package com.green.greengram.feed.like;

import com.green.greengram.config.security.AuthenticationFacade;
import com.green.greengram.entity.Feed;
import com.green.greengram.entity.FeedLike;
import com.green.greengram.entity.FeedLikeIds;
import com.green.greengram.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedLikeService {
    private final FeedLikeRepository repository;
    private final AuthenticationFacade authenticationFacade;

    public int feedLikeToggle(Long feedId) {
        long userId=authenticationFacade.getSignedUserId();
        FeedLikeIds ids = FeedLikeIds.builder()
                .feedId(feedId)
                .userId(authenticationFacade.getSignedUserId())
                .build();

        FeedLike feedLike=repository.findById(ids).orElse(null);
        //영속성이 있는 객체: entity manager가 관리하는 객체, 주소를 알고있는 객체
        if(feedLike==null) {
            Feed feed=Feed.builder()
                    .feedId(feedId)
                    .build();
            User user=User.builder()
                    .userId(userId)
                    .build();

            feedLike=FeedLike.builder()
                    .feedLikeIds(ids)
                    .user(user)
                    .feed(feed)
                    .build();

            repository.save(feedLike);

            return 1;
        }
        repository.delete(feedLike);
        return 0;

    }
}
