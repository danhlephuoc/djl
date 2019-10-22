/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.examples.training;

import ai.djl.Model;
import ai.djl.basicdataset.Mnist;
import ai.djl.examples.training.util.AbstractTraining;
import ai.djl.examples.training.util.Arguments;
import ai.djl.examples.training.util.TrainingUtils;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataDesc;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.Activation;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.training.dataset.Dataset;
import ai.djl.training.initializer.XavierInitializer;
import ai.djl.training.loss.Loss;
import ai.djl.training.metrics.Accuracy;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.optimizer.learningrate.FactorTracker;
import ai.djl.training.optimizer.learningrate.LearningRateTracker;
import ai.djl.translate.Pipeline;
import java.io.IOException;
import java.nio.file.Paths;

public final class TrainMnist extends AbstractTraining {

    public static void main(String[] args) {
        new TrainMnist().runExample(args);
    }

    @Override
    protected void train(Arguments arguments) throws IOException {
        // Construct neural network
        Block block = constructBlock();

        // setup training configuration
        TrainingConfig config = setupTrainingConfig(arguments);

        // configure input data shape based on batch size
        int batchSize = arguments.getBatchSize();
        int numOfSlices = config.getDevices().length;
        Shape inputShape = new Shape(batchSize / numOfSlices, 28 * 28);

        try (Model model = Model.newInstance()) {
            model.setBlock(block);

            // get training and validation dataset
            Dataset trainingSet = getDataset(model.getNDManager(), Dataset.Usage.TRAIN, arguments);
            Dataset validateSet = getDataset(model.getNDManager(), Dataset.Usage.TEST, arguments);

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.setMetrics(metrics);
                trainer.setTrainingListener(this);

                // initialize trainer
                trainer.initialize(new DataDesc[] {new DataDesc(inputShape)});

                TrainingUtils.fit(trainer, config, trainingSet, validateSet);
            }

            // save model
            if (arguments.getOutputDir() != null) {
                model.save(Paths.get(arguments.getOutputDir()), "mnist");
            }
        }
    }

    private Block constructBlock() {
        return new SequentialBlock()
                .add(Blocks.flattenBlock(28 * 28))
                .add(new Linear.Builder().setOutChannels(128).build())
                .add(Activation.reluBlock())
                .add(new Linear.Builder().setOutChannels(64).build())
                .add(Activation.reluBlock())
                .add(new Linear.Builder().setOutChannels(10).build());
    }

    private TrainingConfig setupTrainingConfig(Arguments arguments) {
        int batchSize = arguments.getBatchSize();
        int numEpoch = arguments.getEpoch();

        FactorTracker factorTracker =
                LearningRateTracker.factorTracker()
                        .optBaseLearningRate(0.01f)
                        .setStep(1000)
                        .optFactor(0.1f)
                        .optWarmUpBeginLearningRate(0.001f)
                        .optWarmUpSteps(200)
                        .optStopFactorLearningRate(1e-4f)
                        .build();
        Optimizer optimizer =
                Optimizer.sgd()
                        .setRescaleGrad(1.0f / batchSize)
                        .setLearningRateTracker(factorTracker)
                        .optWeightDecays(0.001f)
                        .optMomentum(0.9f)
                        .build();

        return new DefaultTrainingConfig(new XavierInitializer())
                .setOptimizer(optimizer)
                .addTrainingMetric(Loss.softmaxCrossEntropyLoss())
                .addTrainingMetric(new Accuracy())
                .setEpoch(numEpoch)
                .setBatchSize(batchSize);
    }

    private Dataset getDataset(NDManager manager, Dataset.Usage usage, Arguments arguments)
            throws IOException {
        Pipeline pipeline = new Pipeline(new ToTensor());

        int batchSize = arguments.getBatchSize();
        long maxIterations = arguments.getMaxIterations();

        Mnist mnist =
                new Mnist.Builder()
                        .setManager(manager)
                        .setUsage(usage)
                        .setRandomSampling(batchSize)
                        .optPipeline(pipeline)
                        .optMaxIteration(maxIterations)
                        .build();
        mnist.prepare();
        if (usage == Dataset.Usage.TRAIN) {
            trainDataSize = (int) Math.min(mnist.size() / batchSize, maxIterations);
        } else {
            validateDataSize = (int) Math.min(mnist.size() / batchSize, maxIterations);
        }
        return mnist;
    }
}
