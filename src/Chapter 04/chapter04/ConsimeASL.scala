package chapter04
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.util.StatCounter
import org.jblas.DoubleMatrix
/**
 * �������ƶ�,��Ʒ�Ƽ�
 */

object ConsimeASL {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    //��������
    val rawData = sc.textFile("ml-100k/u.data")
    //�û�ID  | ӰƬID   | �Ǽ�   | ʱ���	
    //196	    | 242	    |  3   |	881250949
    /*��ȡǰ�����ֶμ� �û�ID  | ӰƬID   | �Ǽ� */
    val rawRatings = rawData.map(_.split("\t").take(3)) //ȡ��ǰ�����ֶ�,������ʱ����ʱ����Ҫ
    //Rating�����������Ӧ�û�ID,��Ʒ(��ӰƬID),ʵ���Ǽ�
    //map������ԭ��user ID,movie ID,�Ǽ�������ת��Ϊ��Ӧ�Ķ���,�Ӷ�������������������鼯
    val ratings = rawRatings.map { case Array(user, movie, rating) => Rating(user.toInt, movie.toInt, rating.toDouble) }
    val userPros = rawRatings.map { case Array(user, movie, rating) => (user.toInt, movie.toInt) }

    /**===================��������ģ���Ƽ�=====================================**/
    //ratings.first()
    //����ѵ���Ƽ�ģ��
    //����˵��:
    //rank:��ӦALSģ�����ڵͽ׽��ƾ����е�������������,ͨ������ȡֵΪ10---200
    //iterations:��Ӧ����ʱ��������,10������һ���ͦ��
    //lambda:�ò�������ģ�͵����򻯹���,�Ӷ�����ģ�͵Ĺ�������0.01,�������Ӧ��ͨ���÷������Ĳ������ݽ��н�����֤����     
    val model = ALS.train(ratings, 50, 10, 0.01) //����MatrFactorizationModel�������ֽ�ģ��
    model.userFeatures //�û�����
    //model.userFeatures.count

    /***********��Ʒ�Ƽ�ģ��Ч��*******************/
    //��MovieLens 100K���ݼ��������Ƶ�Ӱ

    //����һ��������������������֮����������ƶ�,
    def cosineSimilarity(vec1: DoubleMatrix, vec2: DoubleMatrix): Double = {
      //�������ƶ�:���������ĵ��������������ĳ˻�����,���ƶȵ�ȡֵ��-1��1֮��
      //���ƶ�ȡֵ��-1��1֮��,1��ʾ�� ȫ����,0��ʾ���߲����(����������)
      val retur = vec1.dot(vec2) / (vec1.norm2() * vec2.norm2())
      println(">>>>>>>>>>>>" + retur)
      retur
    }
    //����Ʒ567Ϊ����ģ����ȡ�����Ӧ������,
    val itemId = 567
    //���ص�һ�����������ֻ���һ��ֵ(ʵ����,������Ҳֻ����һ��ֵ,Ҳ���Ǹ���Ʒ����������)
    val itemFactor = model.productFeatures.lookup(itemId).head
    println("itemFactorSize:" + itemFactor.length)
    for (f <- itemFactor) {
      println("itemFactor:" + f)
    }
    val K = 10
    //����һ��DoubleMatrix����,Ȼ�����øö��������������Լ������ƶ�
    val itemVector = new DoubleMatrix(itemFactor)
    println("itemVector:" + itemVector.toString())
    cosineSimilarity(itemVector, itemVector) //�����������ƶ�,���������Լ������ƶ�

    /***�������Ʒ�����ƶ���***/
    //�����������Ʒ���������ƶ�
    val sims = model.productFeatures.map {
      case (id, factor) =>
        val factorVector = new DoubleMatrix(factor)
        val sim = cosineSimilarity(factorVector, itemVector)
        (id, sim)
    }
    //����Ʒ�������ƶ�����,Ȼ��ȡ������Ʒ567�����Ƶ�ǰ10����Ʒ, 
    //top����Ordering����,�������Spark���ݼ�ֵ�����ֵ����(Ҳ������similarity����)
    val sortedSims = sims.top(K)(Ordering.by { case (id, similarity) => similarity })
    println(sortedSims.mkString("\n"))

    /**
     * (567,1.0000000000000002)
     * (413,0.7431091494962073)
     * (1471,0.7301420755366541)
     * (288,0.7229307761535951)
     * (201,0.7174894985405192)
     * (895,0.7167458613072292)
     * (403,0.7129522148795585)
     * (219,0.712221807408798)
     * (670,0.7118086627261311)
     * (853,0.7014903255601453)
     */

    /*****��Ӱ����*******/
    val movies = sc.textFile("ml-100k/u.item")
    //��ӰID|��Ӱ����                       |����ʱ��          |
    //1     |Toy Story (1995)|01-Jan-1995||http://us.imdb.com/M/title-exact?Toy%20Story%20(1995)|0|0|0|1|1|1|0|0|0|0|0|0|0|0|0|0|0|0|0
    //��ȡǰ�������� ,�ӵ�ӰID�ͱ���,����Map��ʽ
    val titles = movies.map(line => line.split("\\|").take(2)).map(array => (array(0).toInt, array(1))).collectAsMap()
    /**����Ƽ���������Ʒ**/
    //����Ʒ���ƶ�����,Ȼ��ȡ������Ʒ567�����Ƶ�ǰ10����Ʒ
    //����Ordering����,�������Spark���ݼ�ֵ�����ֵ����(Ҳ������similarity����)

    println(titles(itemId)) //Wes Craven's New Nightmare (1994)
    val sortedSims2 = sims.top(K + 1)(Ordering.by[(Int, Double), Double] { case (id, similarity) => similarity })
    //slice��ȡ��1�п�ʼ��11�е���������
    sortedSims2.slice(1, 11).map { case (id, sim) => (titles(id), sim) }.mkString("\n" + ">>>>>")
    /**
     * (Hideaway (1995),0.6932331537649621)
     * (Body Snatchers (1993),0.6898690594544726)
     * (Evil Dead II (1987),0.6897964975027041)
     * (Alien: Resurrection (1997),0.6891221044611473)
     * (Stephen King's The Langoliers (1995),0.6864214133620066)
     * (Liar Liar (1997),0.6812075443259535)
     * (Tales from the Crypt Presents: Bordello of Blood (1996),0.6754663844488256)
     * (Army of Darkness (1993),0.6702643811753909)
     * (Mystery Science Theater 3000: The Movie (1996),0.6594872765176396)
     * (Scream (1996),0.6538249646863378)
     *
     */

    /**�Ƽ�ģ��Ч��������***/
    //�ҳ��û�789�������۹��ĵ�Ӱ
    val moviesForUser = ratings.keyBy(_.user).lookup(789)
    //�û�789�ҳ���һ������
    val actualRating = moviesForUser.take(1)(0) //
    //Ȼ����ģ�͵�Ԥ������
    val predictedRatingR = model.predict(789, actualRating.product)
    //����ʵ��������Ԥ��������ƽ�����
    val squaredError = math.pow(predictedRatingR - actualRating.rating, 2.0)
    //���ȴ�ratings��ȡ�û�����ƷID
    val usersProducts = ratings.map { case Rating(user, product, rating) => (user, product) }
    //�Ը���"�û�-��Ʒ"����Ԥ��,���õ�RDD��"�û�����ƷID"����Ϊ����,��Ӧ��Ԥ��������Ϊֵ
    val predictions = model.predict(usersProducts).map {
      case Rating(user, product, rating) => ((user, product), rating)
    }
    //�����ʵ������,ͬʱ��ratingsRDD��ӳ������"�û�-��Ʒ"��Ϊ����,ʵ������Ϊ��Ӧ��ֵ,�͵õ�����������֯��ͬ��RDD,
    //��������������,�Դ���һ���µ�RDD
    val ratingsAndPredictions = ratings.map {
      case Rating(user, product, rating) => ((user, product), rating)
    }.join(predictions)
    //������
    val MSE = ratingsAndPredictions.map {
      case ((user, product), (actual, predicted)) => math.pow((actual - predicted), 2)
    }.reduce(_ + _) / ratingsAndPredictions.count
    //
    println("������:Mean Squared Error = " + MSE)
    val RMSE = math.sqrt(MSE) //���������
    println("Root Mean Squared Error = " + RMSE)
    val actualMovies = moviesForUser.map(_.product)
    //���¸��û�789�Ƽ���ǰ10����Ʒ 
    val userId = 789
    val topKRecs = model.recommendProducts(userId, K) //������ģ���û�ID,num����Ҫ�Ƽ�����Ʒ���� 

    val predictedMovies = topKRecs.map(_.product)
    val apk10 = avgPrecisionK(actualMovies, predictedMovies, 10)
    val itemFactors = model.productFeatures.map { case (id, factor) => factor }.collect()

    val itemMatrix = new DoubleMatrix(itemFactors)
    println(itemMatrix.rows, itemMatrix.columns)
    val imBroadcast = sc.broadcast(itemMatrix)
    val allRecs = model.userFeatures.map {
      case (userId, array) =>
        val userVector = new DoubleMatrix(array)
        val scores = imBroadcast.value.mmul(userVector)
        val sortedWithId = scores.data.zipWithIndex.sortBy(-_._1)
        val recommendedIds = sortedWithId.map(_._2 + 1).toSeq
        (userId, recommendedIds)
    }

    //**MLib���õ���������**//*
    // MSE, RMSE and MAE
    import org.apache.spark.mllib.evaluation.RegressionMetrics
    /**
     * ��׼���(Standard error)Ϊ������ֵ����ƽ���͵�ƽ��ֵ��ƽ����,
     * ��Ҳ�ƾ��������(Root mean squared error)������ͬ���������½��еĲ�����Ϊ�Ⱦ��Ȳ���
     */
    // next get all the movie ids per user, grouped by user id
    val userMovies = ratings.map { case Rating(user, product, rating) => (user, product) }.groupBy(_._1)
    //����һ��Ԥ��ֵ��ʵ��ֵ
    val predictedAndTrue = ratingsAndPredictions.map { case ((user, product), (actual, predicted)) => (actual, predicted) }
    //ʵ����һ��RegressionMetrics������Ҫһ����ֵ�����͵�RDD,��ÿһ����¼��Ӧÿ�����ݵ�����Ӧ��Ԥ��ֵ��ʵ��ֵ
    val regressionMetrics = new RegressionMetrics(predictedAndTrue)
    //������
    println("Mean Squared Error = " + regressionMetrics.meanSquaredError)
    //���������
    println("Root Mean Squared Error = " + regressionMetrics.rootMeanSquaredError)
    // Mean Squared Error = 0.08231947642632852
    // Root Mean Squared Error = 0.2869137090247319

    // MAPK ׼ȷ��
    //RankingMetrics�����������������������ָ��
    import org.apache.spark.mllib.evaluation.RankingMetrics
    //��Ҫ������֮ǰ��ƽ��׼ȷ�ʺ�������һ����ֵ�����͵�RDD,
    //���Ϊ�����û�Ԥ�����Ʒ��ID����,��ֵ����ʵ�ʵ���ƷID����
    val predictedAndTrueForRanking = allRecs.join(userMovies).map {
      case (userId, (predicted, actualWithIds)) =>
        val actual = actualWithIds.map(_._2)
        (predicted.toArray, actual.toArray)
    }
    //ʹ��RankingMetrics������������������ָ��,��Ҫ������֮ǰƽ���ʺ�������һ����ֵ�����͵�RDD
    //���Ϊ�����û�Ԥ����Ƽ���Ʒ��ID����,��ֵ��ʵ�ʵ���ƷID����
    val rankingMetrics = new RankingMetrics(predictedAndTrueForRanking)
    //ƽ����ȷ��ֵ meanAveragePrecision
    println("ƽ����ȷ��ֵ:Mean Average Precision = " + rankingMetrics.meanAveragePrecision)
    // Mean Average Precision = 0.07171412913757183

    // Compare to our implementation, using K = 2000 to approximate the overall MAP
    //
    val MAPK2000 = allRecs.join(userMovies).map {
      case (userId, (predicted, actualWithIds)) =>
        val actual = actualWithIds.map(_._2).toSeq
        avgPrecisionK(actual, predicted, 2000)
    }.reduce(_ + _) / allRecs.count
    println("Mean Average Precision = " + MAPK2000)
    // Mean Average Precision = 0.07171412913757186

  }
  def avgPrecisionK(actual: Seq[Int], predicted: Seq[Int], k: Int): Double = {
    val predK = predicted.take(k)
    var score = 0.0
    var numHits = 0.0
    //
    for ((p, i) <- predK.zipWithIndex) {
      if (actual.contains(p)) {
        numHits += 1.0
        score += numHits / (i.toDouble + 1.0)
      }
    }
    if (actual.isEmpty) {
      1.0
    } else {
      score / scala.math.min(actual.size, k).toDouble
    }
  }
}