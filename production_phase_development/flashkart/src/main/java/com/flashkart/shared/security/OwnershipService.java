package com.flashkart.shared.security;

import com.flashkart.identity.infra.UserRepository;

import java.util.UUID;

//import com.flashkart.order.infra.OrderRepository; // create/use if exists
import org.springframework.stereotype.Component;

@Component("ownership")
public class OwnershipService {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    //private final OrderRepository orderRepository;

    public OwnershipService(CurrentUser currentUser,
                            UserRepository userRepository
                            /*OrderRepository orderRepository*/) {
        this.currentUser = currentUser;
        this.userRepository = userRepository;
        //this.orderRepository = orderRepository;
    }

    // ABAC: can access user profile
    public boolean isSelf(UUID userId) {
        UUID me = currentUser.userId().orElse(null);
        if (me == null) return false;
        return me.equals(userId);
    }

    /* TODO: Uncomment this when order repository is created
    // NOTE:
    // ownsOrder(orderId) ko baad me add karenge jab Order module (Order entity + repo) create ho jayega.
    // ABAC: order belongs to current user
    public boolean ownsOrder(Long orderId) {
        Long me = currentUser.userId().orElse(null);
        if (me == null) return false;

        // Order table should have userId / customerId
        return orderRepository.existsByIdAndUserId(orderId, me);
    }
    */
}
