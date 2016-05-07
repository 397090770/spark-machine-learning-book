package chapter04
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.util.StatCounter

/**
 * A simple Spark app in Scala
 * ����Spark�Ƽ�����
 */
object ScalaApp4 {

  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    //��������
    val rawData = sc.textFile("ml-100k/u.data")
    //�û�ID  | ӰƬID   | �Ǽ�   | ʱ���	
    //196	    | 242	    |  3   |	881250949
    /*��ȡǰ�����ֶμ� �û�ID  | ӰƬID   | �Ǽ� */
    val rawRatings = rawData.map(_.split("\t").take(3))
    //Rating�����������Ӧ�û�ID,��Ʒ(��ӰƬID),ʵ���Ǽ�
    //map������ԭ��user ID,movie ID,�Ǽ�������ת��Ϊ��Ӧ�Ķ���,�Ӷ�������������������鼯
    val ratings = rawRatings.map { case Array(user, movie, rating) => Rating(user.toInt, movie.toInt, rating.toDouble) }
    /**===================��������ģ���Ƽ�=====================================**/
    //ratings.first()
    //����ѵ���Ƽ�ģ��
    //����˵��:rank��ӦALSģ���е����Ӹ���,ͨ������ȡֵΪ10---200
    //iterations:��Ӧ����ʱ��������,10������һ���ͦ��
    //lambda:�ò�������ģ�͵����򻯹���,�Ӷ�����ģ�͵Ĺ�������0.01 
    val model = ALS.train(ratings, 50, 10, 0.01) //����MatrFactorizationModel����
    model.userFeatures //�û�����
    model.userFeatures.count
    model.productFeatures.count //��Ʒ���� 
    /**=================ʹ���Ƽ�ģ��==============================**/
    //��ģ��Ԥ���û�789�Ե�Ӱ123������Ϊ3.12 
    val predictedRating = model.predict(789, 123) //����ؼ�������û��Ը�����Ʒ��Ԥ�ڵ÷�
    println("Ԥ���û�789�Ե�Ӱ123������Ϊ:" + predictedRating)
    val userId = 789
    val K = 10
    //���¸��û�789�Ƽ���ǰ10����Ʒ 
    val topKRecs = model.recommendProducts(userId, K) //�����û�ID,num����Ҫ�Ƽ�����Ʒ���� 
    println("�û�789�Ƽ���ǰ10����Ʒ\t" + topKRecs.mkString("\n"))

    /*****�����Ƽ�����*******/
    val movies = sc.textFile("ml-100k/u.item")
    //��ӰID|��Ӱ����                       |����ʱ��          |
    //1     |Toy Story (1995)|01-Jan-1995||http://us.imdb.com/M/title-exact?Toy%20Story%20(1995)|0|0|0|1|1|1|0|0|0|0|0|0|0|0|0|0|0|0|0

    //��ȡǰ�������� ,�ӵ�ӰID�ͱ���
    val titles = movies.map(line => line.split("\\|").take(2)).map(array => (array(0).toInt, array(1))).collectAsMap()
    println("��ӰID 123����:" + titles(123))
    //�ҳ��û�789���Ӵ����ĵ�Ӱ
    val moviesForUser = ratings.keyBy(_.user).lookup(789)
    //�鿴����û������˶��ٵ�Ӱ
    println("�û�789�����˶��ٵ�Ӱ:" + moviesForUser.size)
    //�û�789�Ե�Ӱ�����������н�������,�������������ǰ10����Ӱ������
    moviesForUser.sortBy(-_.rating).take(10).map(rating => (titles(rating.product), rating.rating)).foreach(println)
    //��һ��ǰ10���Ƽ�
    println("=========��һ��ǰ10���Ƽ�=============")
    topKRecs.map(rating => (titles(rating.product), rating.rating)).foreach(println)
    /************�Ƽ�����ģ��Ч��*******************/
     //�û�789�ҳ���һ������
    val actualRating = moviesForUser.take(1)(0)//
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
    //
    val MSE = ratingsAndPredictions.map {
      case ((user, product), (actual, predicted)) => math.pow((actual - predicted), 2)
    }.reduce(_ + _) / ratingsAndPredictions.count
    println("Mean Squared Error = " + MSE)
    val RMSE = math.sqrt(MSE)
    println("Root Mean Squared Error = " + RMSE)
    val actualMovies = moviesForUser.map(_.product)
    val predictedMovies = topKRecs.map(_.product)
    val apk10 = avgPrecisionK(actualMovies, predictedMovies, 10)
    val itemFactors = model.productFeatures.map { case (id, factor) => factor }.collect()
    /*
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
    }*/
  }

  def avgPrecisionK(actual: Seq[Int], predicted: Seq[Int], k: Int): Double = {
    val predK = predicted.take(k)
    var score = 0.0
    var numHits = 0.0
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
