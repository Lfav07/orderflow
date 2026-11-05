package com.orderflow.inventory.infrastructure.grpc;

import com.orderflow.inventory.application.command.StockOperationCommand;
import com.orderflow.inventory.application.usecase.DebitStockUseCase;
import com.orderflow.inventory.application.usecase.ReturnStockUseCase;
import com.orderflow.inventory.domain.model.StockOperation;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class StockGrpcService  extends  com.orderflow.inventory.grpc.StockServiceGrpc.StockServiceImplBase{
    private final DebitStockUseCase debitStockUseCase;
    private final ReturnStockUseCase returnStockUseCase;
    public StockGrpcService(DebitStockUseCase debitStockUseCase, ReturnStockUseCase returnStockUseCase) {
        this.debitStockUseCase = debitStockUseCase;
        this.returnStockUseCase = returnStockUseCase;
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
}
