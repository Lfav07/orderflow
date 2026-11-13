package com.orderflow.inventory.infrastructure.grpc;

import com.orderflow.inventory.application.command.ProductIdCommand;
import com.orderflow.inventory.application.usecase.GetProductPriceUseCase;
import com.orderflow.inventory.grpc.GetProductPriceRequest;
import com.orderflow.inventory.grpc.GetProductPriceResponse;
import com.orderflow.inventory.grpc.ProductServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
public class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {
    private final GetProductPriceUseCase getProductPriceUseCase;

    public ProductGrpcService(GetProductPriceUseCase getProductPriceUseCase) {
        this.getProductPriceUseCase = getProductPriceUseCase;
    }

    @Override
    public void getProductPrice(GetProductPriceRequest request, StreamObserver<GetProductPriceResponse> observer) {
        UUID productId = UUID.fromString(request.getProductId());
        var price = getProductPriceUseCase.execute(new ProductIdCommand(productId));
        observer.onNext(GetProductPriceResponse.newBuilder()
                .setProductId(productId.toString())
                .setUnitPrice(price.toPlainString())
                .build());
        observer.onCompleted();
    }
}
