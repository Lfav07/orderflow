package com.orderflow.inventory.infrastructure.grpc;

import com.orderflow.inventory.application.command.StockOperationCommand;
import com.orderflow.inventory.application.command.ChangeStockCommand;
import com.orderflow.inventory.application.usecase.CheckStockAvailabilityUseCase;
import com.orderflow.inventory.application.usecase.DebitStockUseCase;
import com.orderflow.inventory.application.usecase.ReturnStockUseCase;
import com.orderflow.inventory.domain.model.StockOperation;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;

@GrpcService
public class StockGrpcService  extends  com.orderflow.inventory.grpc.StockServiceGrpc.StockServiceImplBase{
    private final DebitStockUseCase debitStockUseCase;
    private final ReturnStockUseCase returnStockUseCase;
    private final CheckStockAvailabilityUseCase checkStockAvailabilityUseCase;
    public StockGrpcService(DebitStockUseCase debitStockUseCase, ReturnStockUseCase returnStockUseCase,
                            CheckStockAvailabilityUseCase checkStockAvailabilityUseCase) {
        this.debitStockUseCase = debitStockUseCase;
        this.returnStockUseCase = returnStockUseCase;
        this.checkStockAvailabilityUseCase = checkStockAvailabilityUseCase;
    }
    @Override
    public void debitStock(com.orderflow.inventory.grpc.StockOperationRequest request, StreamObserver<com.orderflow.inventory.grpc.StockOperationResponse> streamObserver){
        StockOperationCommand command = StockGrpcMapper.toStockOperationCommand(request);
        StockOperation operation = debitStockUseCase.execute(command);
        com.orderflow.inventory.grpc.StockOperationResponse response = StockGrpcMapper.toStockOperationResponse(operation);
        streamObserver.onNext(response);
        streamObserver.onCompleted();
    }
    @Override
    public void returnStock(com.orderflow.inventory.grpc.StockOperationRequest request, StreamObserver<com.orderflow.inventory.grpc.StockOperationResponse> streamObserver){
        StockOperationCommand command = StockGrpcMapper.toStockOperationCommand(request);
        StockOperation operation = returnStockUseCase.execute(command);
        com.orderflow.inventory.grpc.StockOperationResponse response = StockGrpcMapper.toStockOperationResponse(operation);
        streamObserver.onNext(response);
        streamObserver.onCompleted();
    }

    @Override
    public void checkStockAvailability(com.orderflow.inventory.grpc.CheckStockAvailabilityRequest request,
                                       StreamObserver<com.orderflow.inventory.grpc.CheckStockAvailabilityResponse> observer) {
        UUID productId = UUID.fromString(request.getProductId());
        BigDecimal quantity = new BigDecimal(request.getQuantity());
        boolean available = checkStockAvailabilityUseCase.execute(new ChangeStockCommand(productId, quantity));
        observer.onNext(com.orderflow.inventory.grpc.CheckStockAvailabilityResponse.newBuilder()
                .setProductId(productId.toString())
                .setAvailable(available)
                .build());
        observer.onCompleted();
    }
}
