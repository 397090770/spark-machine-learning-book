package chapter06

import org.apache.spark.SparkConf

import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.LogisticRegressionWithSGD
import org.apache.spark.mllib.classification.NaiveBayes
import org.apache.spark.mllib.classification.SVMWithSGD
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.tree.configuration.Algo
import org.apache.spark.mllib.tree.impurity.Entropy
import org.apache.spark.mllib.tree.impurity.Impurity
import org.apache.spark.rdd.RDD
/**
 * �����ع�ģ��----�����͵�����
 */
object AppScala6 {

  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    /**ÿ��Сʱ���г��������****/
    val rawData = sc.textFile("BikeSharingDataset/hour_noheader.csv")
    val records = rawData.map(line => line.split(","))
    records.count
    //res1: Long = 17379
    records.first()
    //res0: Array[String] = Array(1, 2011-1-1, 1, 0, 1, 0, 0, 6, 0, 1, 0.24, 0.2879, 0.81, 0, 3, 13, 16)
    //������idx��ֵȥ��,Ȼ���ÿ��ֵʹ��zipWithIndex����ӳ�䵽һ��Ψһ������,���������һ��RDD�ļ�ֵӳ��,���Ǳ���,ֵ������
    //�ļ�����Ϣ(��,��,��,��)
    records.map(fields => fields(2)).distinct().zipWithIndex().collectAsMap()
    //res9: scala.collection.Map[String,Long] = Map(2 -> 1, 1 -> 3, 4 -> 0, 3 -> 2)
    //������idx��ֵȥ��,Ȼ���ÿ��ֵʹ��zipWithIndex����ӳ�䵽һ��Ψһ������,���������һ��RDD�ļ�ֵӳ��,���Ǳ���,ֵ������
    def get_mapping(rdd: RDD[Array[String]], idx: Int): scala.collection.Map[String, Long] = {
      rdd.map(fields => fields(idx)).distinct().zipWithIndex().collectAsMap()
    }
    
    val mappings=for( i<-2 to 10){
      get_mapping(records,i)
    }


    /***�������г�ȡ���ʵ�����**/
    // val records = rawData.map(line => line.split("\t"))
    //��ʼ���зֱ����URL��ҳ���ID��ԭʼ���ı����ݺͷ����ҳ������
    // records.first
    //Array[String] = Array("http://www.bloomberg.com/news/2010-12-23/ibm-predicts-holographic-calls-air-breathing-batteries-by-2015.html", "4042", ...
    //�������ݸ�ʽ�����⣬������һЩ��������Ĺ������Ѷ����(��)ȥ������   
    val data = records.map { r =>
      //�Ѷ����(��)ȥ��
      val trimmed = r.map(_.replaceAll("\"", ""))
      /*  val trimmed = r.map { x =>
        //println("befor:" + x)
        val v = x.replaceAll("\"", "")
        // println("after:" + v)
        v
      }*/

      //println("r:" + r.toList + "\t size:" + r.size)
      //r��Array[String] = Array("http://www.bloomberg.com/news/2010-12-23/ibm-predicts-holographic-calls-air-breathing-batteries-by-2015.html", "4042", ...
      //����һ������Ϊ0��ʼ      
      //println(r.size - 1 + ":\t��һ��:" + r(0) + ":\t���һ������size:" + r(r.size - 1))
      /* r.foreach {
        var i = 0
        x =>
          println(i + ">>>" + x)
          i += 1
      }*/
      //r.size - 1��������"\"",�滻�� ""    
      //ȡ�����һ��ֵת����toInt,һ��0��1
      val label = trimmed(r.size - 1).toInt
      println("(���һ��ֵ)label:" + label)
      //����һ�������������������������������ֵ����,ȡ�Ӽ�set(1,4ΪԪ��λ��, ��0��ʼ),��λ��4��ʼ,������ĳ���     
      //slice��ȡ��5�п�ʼ��25�е���������
      //���ݼ���ȱʧ����Ϊ?,ֱ����0�滻ȱʧ����
      val features = trimmed.slice(4, r.size - 1).map {
        d =>
          //println("" + d)
          if (d == "?") {
            //println(d)
            0.0
          } else {
            d.toDouble
          }
      }
      //����ǩ����������ת��ΪLabeledPointΪʵ��,�����������洢��MLib��Vectors��,labelһ����0��1
      LabeledPoint(label, Vectors.dense(features))
    }
    data.cache
    // numData: Long = 7395
    val numData = data.count
    // train a Logistic Regression model    
    // note that some of our data contains negative feature vaues. For naive Bayes we convert these to zeros
    //�������ر�Ҷ˹ģ��,Ҫ������ֵ�Ǹ�,����������������ֵ������׳�����
    val nbData = records.map { r =>
      val trimmed = r.map(_.replaceAll("\"", ""))
      val label = trimmed(r.size - 1).toInt
      //������һ��С��0,��Ϊ0
      val features = trimmed.slice(4, r.size - 1).map(d => if (d == "?") 0.0 else d.toDouble).map(d => if (d < 0) 0.0 else d)
      LabeledPoint(label, Vectors.dense(features))
    }

    /***����ѵ������ģ��**/
    //�����߼��ع��SVM��������
    val numIterations = 10
    //������������
    val maxTreeDepth = 5
    //�����߼��ع�ģ��
    val lrModel = LogisticRegressionWithSGD.train(data, numIterations)
    //����ѵ��SVMģ��
    val svmModel = SVMWithSGD.train(data, numIterations)
    //�������ر�Ҷ˹ģ��,ʹ��û�и�����������
    // note we use nbData here for the NaiveBayes model training
    val nbModel = NaiveBayes.train(nbData)
    //����������
    val dtModel = DecisionTree.train(data, Algo.Classification, Entropy, maxTreeDepth)

    /***ʹ�÷���ģ��Ԥ��**/
    // make prediction on a single data point    
    val dataPoint = data.first
    //�߼�ģ��Ԥ��
    // dataPoint: org.apache.spark.mllib.regression.LabeledPoint = LabeledPoint(0.0, [0.789131,2.055555556,0.676470588, ...
    //ѵ�������е�һ������,ģ��Ԥ��ֵΪ1,������
    val prediction = lrModel.predict(dataPoint.features)
    //ģ��Ԥ�������
    // prediction: Double = 1.0
    //����һ��������������ı�ǩ 
    val trueLabel = dataPoint.label
    // trueLabel: Double = 0.0
    //��RDD[Vector]������Ϊ������Ԥ��
    val predictions = lrModel.predict(data.map(lp => lp.features))
    predictions.take(5)
    // res1: Array[Double] = Array(1.0, 1.0, 1.0, 1.0, 1.0)
    //SVMģ��
    val predictionsSvmModel = svmModel.predict(data.map(lp => lp.features))
    predictionsSvmModel.take(5)
    //NaiveBayesģ�����ر�Ҷ˹
    val predictionsNbModel = nbModel.predict(data.map(lp => lp.features))
    predictionsNbModel.take(5)
    ///����������
    val predictionsDtModel = dtModel.predict(data.map(lp => lp.features))
    predictionsDtModel.take(5)

    /***��������ģ�͵�����**/
    /**
     * ��������ʹ�õ�������������
     * 1)Ԥ����ȷ�ʺʹ�����
     * 2)׼ȷ�ʺ��ٻ���
     * 3)׼ȷ��һ�ٻ��������·������ROC����
     * 4)׼ȷ�ʺ��ٻ���
     *
     */
    //Ԥ�����ȷ�ʺʹ�����
    //��ȷ��=ѵ�������б���ȷ�������Ŀ/��������
    //������=ѵ�������б���������������Ŀ/��������

    //�߼��ع�ģ�Ͷ���������Ԥ��ֵ��ʵ�ʱ�ǩ���бȽ����
    val lrTotalCorrect = data.map { point =>
      //point.labelʵ�ʱ�ǩ���бȽ�
      if (lrModel.predict(point.features) == point.label) 1 else 0
    }.sum
    // lrTotalCorrect: Double = 3806.0
    /**��ȷ�� ������ȷ�����������Ŀ��Ͳ�������������,�߼��ع�ģ�͵õ���ȷ��51.5%**/
    val lrAccuracy = lrTotalCorrect / numData
    // lrAccuracy: Double = 0.5146720757268425       

    //SVMģ�Ͷ���������Ԥ��ֵ��ʵ�ʱ�ǩ���бȽ����
    val svmTotalCorrect = data.map { point =>
      //point.labelʵ�ʱ�ǩ���бȽ�
      if (svmModel.predict(point.features) == point.label) 1 else 0
    }.sum
    //���ر�Ҷ˹ģ�Ͷ���������Ԥ��ֵ��ʵ�ʱ�ǩ���бȽ����
    val nbTotalCorrect = nbData.map { point =>
      if (nbModel.predict(point.features) == point.label) 1 else 0
    }.sum
    //������ģ�Ͷ���������Ԥ��ֵ��ʵ�ʱ�ǩ���бȽ����
    // decision tree threshold needs to be specified
    val dtTotalCorrect = data.map { point =>
      val score = dtModel.predict(point.features)
      //��������Ԥ����ֵ��Ҫ��ȷ���� 0.5
      val predicted = if (score > 0.5) 1 else 0
      if (predicted == point.label) 1 else 0
    }.sum
    //Ԥ��SVMģ����ȷ��,�߼��ع�ģ�͵õ���ȷ��51.4%/
    val svmAccuracy = svmTotalCorrect / numData
    //svmAccuracy: Double = 0.5146720757268425
    //Ԥ�����ر�Ҷ˹ģ����ȷ��58%
    val nbAccuracy = nbTotalCorrect / numData
    // nbAccuracy: Double = 0.5803921568627451
    //������˹ģ����ȷ�� 65%
    val dtAccuracy = dtTotalCorrect / numData
    // dtAccuracy: Double = 0.6482758620689655
    /***���� SVM�����ر�Ҷ˹ģ�����ܶ��ϲ�,��������ģ����ȷ��65%,�������Ǻܸ�***/

    /**
     * *
     * ׼ȷ�ʺ��ٻ���
     * ׼ȷ��ͨ���������۽��������,�ٻ����������۽����������
     * ������׼ȷ��=�����Ե���Ŀ���������Ժͼ����Ե�����,
     * ��������ָ����ȷԤ������Ϊ1������
     * ��������ָ������Ԥ������Ϊ1������
     * *
     */
    //����������PR(�ٻ���)��ROC�����µ����
    val metrics = Seq(lrModel, svmModel).map { model =>
      val scoreAndLabels = data.map { point =>
        (model.predict(point.features), point.label)
      }
      val metrics = new BinaryClassificationMetrics(scoreAndLabels)
      (model.getClass.getSimpleName, metrics.areaUnderPR, metrics.areaUnderROC)
    }
    // again, we need to use the special nbData for the naive Bayes metrics 
    val nbMetrics = Seq(nbModel).map { model =>
      val scoreAndLabels = nbData.map { point =>
        val score = model.predict(point.features)
        (if (score > 0.5) 1.0 else 0.0, point.label)
      }
      val metrics = new BinaryClassificationMetrics(scoreAndLabels)
      (model.getClass.getSimpleName, metrics.areaUnderPR, metrics.areaUnderROC)
    }
    // here we need to compute for decision tree separately since it does 
    // not implement the ClassificationModel interface
    val dtMetrics = Seq(dtModel).map { model =>
      val scoreAndLabels = data.map { point =>
        val score = model.predict(point.features)
        (if (score > 0.5) 1.0 else 0.0, point.label)
      }
      val metrics = new BinaryClassificationMetrics(scoreAndLabels)
      (model.getClass.getSimpleName, metrics.areaUnderPR, metrics.areaUnderROC)
    }
    val allMetrics = metrics ++ nbMetrics ++ dtMetrics
    allMetrics.foreach {
      case (m, pr, roc) =>
        println(f"$m, Area under PR: ${pr * 100.0}%2.4f%%, Area under ROC: ${roc * 100.0}%2.4f%%")
    }
    /*
          ƽ��׼ȷ��,�õ�ģ�͵�ƽ���ʶ����
    LogisticRegressionModel, Area under PR: 75.6759%, Area under ROC: 50.1418%
    SVMModel, Area under PR: 75.6759%, Area under ROC: 50.1418%
    NaiveBayesModel, Area under PR: 68.0851%, Area under ROC: 58.3559%
    DecisionTreeModel, Area under PR: 74.3081%, Area under ROC: 64.8837%
    */

    /***�������ݱ�׼��****/
    import org.apache.spark.mllib.linalg.distributed.RowMatrix
    /**������������RowMatrix���ʾ��MLib�еķֲ�ʽ����,RowMatrix��һ����������ɵ�RDD,����ÿ�������Ƿֲ������һ��**/
    val vectors = data.map {
      lp =>
        //ÿ�е�����
        println("lp.features:" + lp.features)
        lp.features
    }
    val matrix = new RowMatrix(vectors)
    //�������ÿ�е�ͳ������ 
    val matrixSummary = matrix.computeColumnSummaryStatistics()
    //����ÿ�е�ƽ��ֵ
    println(matrixSummary.mean)
    // [0.41225805299526636,2.761823191986623,0.46823047328614004, ...
    //����ÿ�е���Сֵ
    println(matrixSummary.min)
    // [0.0,0.0,0.0,0.0,0.0,0.0,0.0,-1.0,0.0,0.0,0.0,0.045564223,-1.0, ...
    //����ÿ�е����ֵ
    println(matrixSummary.max)
    // [0.999426,363.0,1.0,1.0,0.980392157,0.980392157,21.0,0.25,0.0,0.444444444, ...
    //����ÿ�еķ���,���ֵ�һ������;�ֵ���Ƚϸ�,�����ϱ�׼�ĸ�˹�ֲ�
    println(matrixSummary.variance)
    // [0.1097424416755897,74.30082476809638,0.04126316989120246, ...
    //����ÿ���еķ�0������Ŀ
    println(matrixSummary.numNonzeros)
    //��ÿ���������б�׼��,ʹ��ÿ��������0��ֵ�͵�λ��׼��,���������Ƕ�ÿ������ֵ��ȥ�еľ�ֵ,Ȼ������еı�׼��
    //��������
    import org.apache.spark.mllib.feature.StandardScaler
    //��һ�������Ƿ�������м�ȥ��ֵ,��һ����ʾ�Ƿ�Ӧ�ñ�׼������,���ع�һ��������
    val scaler = new StandardScaler(withMean = true, withStd = true).fit(vectors)
    //ʹ��Map����LabeledPoint���ݼ��ı�ǩ,transform���ݱ�׼��
    val scaledData = data.map(lp => LabeledPoint(lp.label, scaler.transform(lp.features)))
    // compare the raw features with the scaled features
    //��׼��֮ǰ����
    println(data.first.features)
    // [0.789131,2.055555556,0.676470588,0.205882353,
    //��׼��֮�������
    println(scaledData.first.features)
    // [1.1376439023494747,-0.08193556218743517,1.025134766284205,-0.0558631837375738,
    //���Կ�����һ�������Ѿ�Ӧ�ñ�׼�ʽ��ת��,��֤��һ������ֵ,��һ����ֵ��ȥ��ֵ,Ȼ����Ա�׼��(�����ƽ����)
    println((0.789131 - 0.41225805299526636) / math.sqrt(0.1097424416755897))
    // 1.137647336497682
    /***��׼����������ѵ��ģ��,����ֻѵ���߼��ع�(�����������ر�Ҷ˹����������׼����Ӱ��),��˵��������׼����Ӱ��***/
    //�����߼��ع�ģ��
    val lrModelScaled = LogisticRegressionWithSGD.train(scaledData, numIterations)
    //�߼��ع�ģ�Ͷ���������Ԥ��ֵ��ʵ�ʱ�ǩ���бȽ����
    val lrTotalCorrectScaled = scaledData.map { point =>
      if (lrModelScaled.predict(point.features) == point.label) 1 else 0
    }.sum
    /**��ȷ�� ������ȷ�����������Ŀ��Ͳ�������������,�߼��ع�ģ�͵õ���ȷ��62%**/
    val lrAccuracyScaled = lrTotalCorrectScaled / numData
    // lrAccuracyScaled: Double = 0.6204192021636241

    val lrPredictionsVsTrue = scaledData.map { point =>
      //
      (lrModelScaled.predict(point.features), point.label)
    }
    val lrMetricsScaled = new BinaryClassificationMetrics(lrPredictionsVsTrue)
    //׼ȷ�ʺ��ٻ���
    val lrPr = lrMetricsScaled.areaUnderPR
    //
    val lrRoc = lrMetricsScaled.areaUnderROC
    println(f"${lrModelScaled.getClass.getSimpleName}\n ��ȷ��Accuracy: ${lrAccuracyScaled * 100}%2.4f%%\nArea under PR: ${lrPr * 100.0}%2.4f%%\nArea under ROC: ${lrRoc * 100.0}%2.4f%%")

    /*
           �ӽ�����Կ���,ͨ���򵥵�������׼��,��������߼��ع��׼ȷ��,����AUC�����50%,������62%
    LogisticRegressionModel
    Accuracy: 62.0419%
    Area under PR: 72.7254%
    Area under ROC: 61.9663%
    */

    /**��������������ģ��Ӱ��**/

    // Investigate the impact of adding in the 'category' feature
    //ÿ�������һ��ȥ������ӳ��,�������������������������1-of-K����
    val categories = records.map(r => r(3)).distinct.collect.zipWithIndex.toMap
    // categories: scala.collection.immutable.Map[String,Int] = Map("weather" -> 0, "sports" -> 6, 
    //	"unknown" -> 4, "computer_internet" -> 12, "?" -> 11, "culture_politics" -> 3, "religion" -> 8,
    // "recreation" -> 2, "arts_entertainment" -> 9, "health" -> 5, "law_crime" -> 10, "gaming" -> 13, 
    // "business" -> 1, "science_technology" -> 7)

    val numCategories = categories.size
    // numCategories: Int = 14
    //������Ҫ����һ����Ϊ14��������ʾ�������,Ȼ�����ÿ�����������������,��Ӧ��Ӧ��ά�ȸ�ֵΪ1,����Ϊ0
    val dataCategories = records.map { r =>
      //�Ѷ����(��)ȥ��
      val trimmed = r.map(_.replaceAll("\"", ""))
      //ȡ�����һ��ֵת����toInt,һ��0��1
      val label = trimmed(r.size - 1).toInt
      //����ÿ�����������������,��Ӧ��Ӧ��ά�ȸ�ֵΪ1,����Ϊ0
      val categoryIdx = categories(r(3))
      //ofDim�������м��ж�ά����
      val categoryFeatures = Array.ofDim[Double](numCategories)
      categoryFeatures(categoryIdx) = 1.0
      //����һ�������������������������������ֵ����,ȡ�Ӽ�set(1,4ΪԪ��λ��, ��0��ʼ),��λ��4��ʼ,������ĳ���     
      //slice��ȡ��5�п�ʼ��25�е���������
      val otherFeatures = trimmed.slice(4, r.size - 1).map(d => if (d == "?") 0.0 else d.toDouble)
      val features = categoryFeatures ++ otherFeatures
      LabeledPoint(label, Vectors.dense(features))
    }
    println(dataCategories.first)
    // LabeledPoint(0.0, [0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.789131,2.055555556,
    //	0.676470588,0.205882353,0.047058824,0.023529412,0.443783175,0.0,0.0,0.09077381,0.0,0.245831182,
    // 0.003883495,1.0,1.0,24.0,0.0,5424.0,170.0,8.0,0.152941176,0.079129575])
    // ʹ��StandardScaler�������б�׼��ת��
    val scalerCats = new StandardScaler(withMean = true, withStd = true).fit(dataCategories.map(lp => lp.features))
    val scaledDataCats = dataCategories.map(lp => LabeledPoint(lp.label, scalerCats.transform(lp.features)))
    println(dataCategories.first.features)
    // [0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.789131,2.055555556,0.676470588,0.205882353,
    // 0.047058824,0.023529412,0.443783175,0.0,0.0,0.09077381,0.0,0.245831182,0.003883495,1.0,1.0,24.0,0.0,
    // 5424.0,170.0,8.0,0.152941176,0.079129575]
    println(scaledDataCats.first.features)
    /*
    [-0.023261105535492967,2.720728254208072,-0.4464200056407091,-0.2205258360869135,-0.028492999745483565,
    -0.2709979963915644,-0.23272692307249684,-0.20165301179556835,-0.09914890962355712,-0.381812077600508,
    -0.06487656833429316,-0.6807513271391559,-0.2041811690290381,-0.10189368073492189,1.1376439023494747,
    -0.08193556218743517,1.0251347662842047,-0.0558631837375738,-0.4688883677664047,-0.35430044806743044
    ,-0.3175351615705111,0.3384496941616097,0.0,0.8288021759842215,-0.14726792180045598,0.22963544844991393,
    -0.14162589530918376,0.7902364255801262,0.7171932152231301,-0.29799680188379124,-0.20346153667348232,
    -0.03296720969318916,-0.0487811294839849,0.9400696843533806,-0.10869789547344721,-0.2788172632659348]
    */
    /***�������� **/
    // train model on scaled data and evaluate metrics
    val lrModelScaledCats = LogisticRegressionWithSGD.train(scaledDataCats, numIterations)
    val lrTotalCorrectScaledCats = scaledDataCats.map { point =>
      if (lrModelScaledCats.predict(point.features) == point.label) 1 else 0
    }.sum
    //׼ȷ��
    val lrAccuracyScaledCats = lrTotalCorrectScaledCats / numData
    val lrPredictionsVsTrueCats = scaledDataCats.map { point =>
      (lrModelScaledCats.predict(point.features), point.label)
    }
    val lrMetricsScaledCats = new BinaryClassificationMetrics(lrPredictionsVsTrueCats)
    val lrPrCats = lrMetricsScaledCats.areaUnderPR
    val lrRocCats = lrMetricsScaledCats.areaUnderROC
    println(f"${lrModelScaledCats.getClass.getSimpleName}\nAccuracy: ${lrAccuracyScaledCats * 100}%2.4f%%\nArea under PR: ${lrPrCats * 100.0}%2.4f%%\nArea under ROC: ${lrRocCats * 100.0}%2.4f%%")
    /*
     * �ܽ�����ݱ�׼��,ģ��׼ȷ�ʵõ�����,��50%������62%,֮���������,ģ�����ܽ�һ��������65%(��������ӵ�����Ҳ���˱�׼������)
    LogisticRegressionModel
    Accuracy: 66.5720%
    Area under PR: 75.7964%
    Area under ROC: 66.5483%
    */
    /**����ѵ�����ر�Ҷ˹ģ��**/
    // train naive Bayes model with only categorical data
    val dataNB = records.map { r =>
      val trimmed = r.map(_.replaceAll("\"", ""))
      val label = trimmed(r.size - 1).toInt
      val categoryIdx = categories(r(3))
      val categoryFeatures = Array.ofDim[Double](numCategories)
      categoryFeatures(categoryIdx) = 1.0
      LabeledPoint(label, Vectors.dense(categoryFeatures))
    }
    val nbModelCats = NaiveBayes.train(dataNB)
    /**����ѵ�����ر�Ҷ˹ģ�Ͷ���������**/
    val nbTotalCorrectCats = dataNB.map { point =>
      if (nbModelCats.predict(point.features) == point.label) 1 else 0
    }.sum
    val nbAccuracyCats = nbTotalCorrectCats / numData
    val nbPredictionsVsTrueCats = dataNB.map { point =>
      (nbModelCats.predict(point.features), point.label)
    }
    val nbMetricsCats = new BinaryClassificationMetrics(nbPredictionsVsTrueCats)
    val nbPrCats = nbMetricsCats.areaUnderPR
    val nbRocCats = nbMetricsCats.areaUnderROC
    println(f"${nbModelCats.getClass.getSimpleName}\nAccuracy: ${nbAccuracyCats * 100}%2.4f%%\nArea under PR: ${nbPrCats * 100.0}%2.4f%%\nArea under ROC: ${nbRocCats * 100.0}%2.4f%%")
    /*
     * ʹ�ø�ʽ��ȷ���������ݺ�,���ر�Ҷ˹�����ܴ�58%������60% 
    NaiveBayesModel
    Accuracy: 60.9601%
    Area under PR: 74.0522%
    Area under ROC: 60.5138%
    */
    /**ģ�Ͳ�������**/
    import org.apache.spark.rdd.RDD
    import org.apache.spark.mllib.optimization.Updater
    import org.apache.spark.mllib.optimization.SimpleUpdater
    import org.apache.spark.mllib.optimization.L1Updater
    import org.apache.spark.mllib.optimization.SquaredL2Updater
    import org.apache.spark.mllib.classification.ClassificationModel
    /**����ģ�Ͳ���**/
    // helper function to train a logistic regresson model
    //��������ѵ��ģ��
    def trainWithParams(input: RDD[LabeledPoint], regParam: Double, numIterations: Int, updater: Updater, stepSize: Double) = {
      val lr = new LogisticRegressionWithSGD
      lr.optimizer.setNumIterations(numIterations).setUpdater(updater).setRegParam(regParam).setStepSize(stepSize)
      lr.run(input)
    }
    // helper function to create AUC metric
    //����ڶ������������������������ݺͷ���ģ��,������ص�AUC
    def createMetrics(label: String, data: RDD[LabeledPoint], model: ClassificationModel) = {
      val scoreAndLabels = data.map { point =>
        (model.predict(point.features), point.label)
      }
      val metrics = new BinaryClassificationMetrics(scoreAndLabels)
      (label, metrics.areaUnderROC)
    }
    //�����׼������,�ӿ���ģ��ѵ�����ٶ�
    // cache the data to increase speed of multiple runs agains the dataset
    scaledDataCats.cache
    // num iterations
    /**��������,(���ܲ���)**/
    val iterResults = Seq(1, 5, 10, 50).map { param =>
      val model = trainWithParams(scaledDataCats, 0.0, param, new SimpleUpdater, 1.0)
      createMetrics(s"$param iterations", scaledDataCats, model)
    }
    iterResults.foreach { case (param, auc) => println(f"$param, AUC = ${auc * 100}%2.2f%%") }
    /*
     * һ������ض������ĵ���,��������������Խ��Ӱ���С
    1 iterations, AUC = 64.97%
    5 iterations, AUC = 66.62%
    10 iterations, AUC = 66.55%
    50 iterations, AUC = 66.81%
    */
    /**��������,(���ܲ���),�������������㷨����ݶȷ�����Ӧ��ǰ����Զ**/
    // step size
    val stepResults = Seq(0.001, 0.01, 0.1, 1.0, 10.0).map { param =>
      val model = trainWithParams(scaledDataCats, 0.0, numIterations, new SimpleUpdater, param)
      createMetrics(s"$param step size", scaledDataCats, model)
    }
    stepResults.foreach { case (param, auc) => println(f"$param, AUC = ${auc * 100}%2.2f%%") }
    /*
     * ������������������и���Ӱ��
    0.001 step size, AUC = 64.95%
    0.01 step size, AUC = 65.00%
    0.1 step size, AUC = 65.52%
    1.0 step size, AUC = 66.55%
    10.0 step size, AUC = 61.92%
    */
    /**����ͨ������ģ�͵ĸ��Ӷȱ���ģ����ѵ�������й����regularization**/
    val regResults = Seq(0.001, 0.01, 0.1, 1.0, 10.0).map { param =>
      val model = trainWithParams(scaledDataCats, param, numIterations, new SquaredL2Updater, 1.0)
      createMetrics(s"$param L2 regularization parameter", scaledDataCats, model)
    }
    regResults.foreach { case (param, auc) => println(f"$param, AUC = ${auc * 100}%2.2f%%") }
    /*
     *����:�ȼ��͵�������ģ�͵�����Ӱ�첻��,Ȼ��,�������򻯿��Կ���Ƿ��ϻᵼ�½ϵ�ģ������ 
    0.001 L2 regularization parameter, AUC = 66.55%
    0.01 L2 regularization parameter, AUC = 66.55%
    0.1 L2 regularization parameter, AUC = 66.63%
    1.0 L2 regularization parameter, AUC = 66.04%
    10.0 L2 regularization parameter, AUC = 35.33%
    */

    /**������ģ�Ͳ���**/
    /**������ģ����һ��ʼʹ��ԭʼ������ѵ��ʱ�������õ�����,��ʱ�����˲���������**/
    import org.apache.spark.mllib.tree.impurity.Entropy
    import org.apache.spark.mllib.tree.impurity.Gini
    def trainDTWithParams(input: RDD[LabeledPoint], maxDepth: Int, impurity: Impurity) = {
      DecisionTree.train(input, Algo.Classification, impurity, maxDepth)
    }

    // investigate tree depth impact for Entropy impurity
    val dtResultsEntropy = Seq(1, 2, 3, 4, 5, 10, 20).map { param =>
      val model = trainDTWithParams(data, param, Entropy)
      val scoreAndLabels = data.map { point =>
        val score = model.predict(point.features)
        (if (score > 0.5) 1.0 else 0.0, point.label)
      }
      val metrics = new BinaryClassificationMetrics(scoreAndLabels)
      (s"$param tree depth", metrics.areaUnderROC)
    }
    dtResultsEntropy.foreach { case (param, auc) => println(f"$param, AUC = ${auc * 100}%2.2f%%") }
    /*
     * 
      1 tree depth, AUC = 59.33%
      2 tree depth, AUC = 61.68%
      3 tree depth, AUC = 62.61%
      4 tree depth, AUC = 63.63%
      5 tree depth, AUC = 64.88%
      10 tree depth, AUC = 76.26%
      20 tree depth, AUC = 98.45%
      */
    // investigate tree depth impact for Gini impurity
    val dtResultsGini = Seq(1, 2, 3, 4, 5, 10, 20).map { param =>
      val model = trainDTWithParams(data, param, Gini)
      val scoreAndLabels = data.map { point =>
        val score = model.predict(point.features)
        (if (score > 0.5) 1.0 else 0.0, point.label)
      }
      val metrics = new BinaryClassificationMetrics(scoreAndLabels)
      (s"$param tree depth", metrics.areaUnderROC)
    }
    dtResultsGini.foreach { case (param, auc) => println(f"$param, AUC = ${auc * 100}%2.2f%%") }
    /*
     * ����:���������ȿ��Եõ�����ȷ��ģ��,Ȼ���������Խ��,ģ�Ͷ�ѵ�����ݹ���϶�Խ����
     * ���ֲ����ȷ��������ܵ�Ӱ������С 
    1 tree depth, AUC = 59.33%
    2 tree depth, AUC = 61.68%
    3 tree depth, AUC = 62.61%
    4 tree depth, AUC = 63.63%
    5 tree depth, AUC = 64.89%
    10 tree depth, AUC = 78.37%
    20 tree depth, AUC = 98.87%
    */
    /**���ر�Ҷ˹ģ�Ͳ���**/
    // investigate Naive Bayes parameters
    def trainNBWithParams(input: RDD[LabeledPoint], lambda: Double) = {
      val nb = new NaiveBayes
      nb.setLambda(lambda)
      nb.run(input)
    }
    val nbResults = Seq(0.001, 0.01, 0.1, 1.0, 10.0).map { param =>
      val model = trainNBWithParams(dataNB, param)
      val scoreAndLabels = dataNB.map { point =>
        (model.predict(point.features), point.label)
      }
      val metrics = new BinaryClassificationMetrics(scoreAndLabels)
      (s"$param lambda", metrics.areaUnderROC)
    }
    nbResults.foreach { case (param, auc) => println(f"$param, AUC = ${auc * 100}%2.2f%%") }
    /*
     * Lamda���������ر�Ҷ˹ģ�͵�Ӱ��,Lamda���������ĳ������ĳ������ֵ�����û��ͬʱ���ֵ����� 
     * �ܽ�:Lamda��ֵ������û��Ӱ��
    0.001 lambda, AUC = 60.51%
    0.01 lambda, AUC = 60.51%
    0.1 lambda, AUC = 60.51%
    1.0 lambda, AUC = 60.51%
    10.0 lambda, AUC = 60.51%
    */
    /**������֤����**/
    // illustrate cross-validation
    // create a 60% / 40% train/test data split
    val trainTestSplit = scaledDataCats.randomSplit(Array(0.6, 0.4), 123)
    val train = trainTestSplit(0)
    val test = trainTestSplit(1)
    // now we train our model using the 'train' dataset, and compute predictions on unseen 'test' data
    // in addition, we will evaluate the differing performance of regularization on training and test datasets
    val regResultsTest = Seq(0.0, 0.001, 0.0025, 0.005, 0.01).map { param =>
      val model = trainWithParams(train, param, numIterations, new SquaredL2Updater, 1.0)
      createMetrics(s"$param L2 regularization parameter", test, model)
    }
    regResultsTest.foreach { case (param, auc) => println(f"$param, AUC = ${auc * 100}%2.6f%%") }
    /*
    0.0 L2 regularization parameter, AUC = 66.480874%
    0.001 L2 regularization parameter, AUC = 66.480874%
    0.0025 L2 regularization parameter, AUC = 66.515027%
    0.005 L2 regularization parameter, AUC = 66.515027%
    0.01 L2 regularization parameter, AUC = 66.549180%
    */

    // training set results
    val regResultsTrain = Seq(0.0, 0.001, 0.0025, 0.005, 0.01).map { param =>
      val model = trainWithParams(train, param, numIterations, new SquaredL2Updater, 1.0)
      createMetrics(s"$param L2 regularization parameter", train, model)
    }
    regResultsTrain.foreach { case (param, auc) => println(f"$param, AUC = ${auc * 100}%2.6f%%") }
    /*
    0.0 L2 regularization parameter, AUC = 66.260311%
    0.001 L2 regularization parameter, AUC = 66.260311%
    0.0025 L2 regularization parameter, AUC = 66.260311%
    0.005 L2 regularization parameter, AUC = 66.238294%
    0.01 L2 regularization parameter, AUC = 66.238294%
    */
  }
}