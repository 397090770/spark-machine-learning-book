package chapter04
import org.apache.log4j.{ Level, Logger }
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel
import org.apache.spark.mllib.recommendation.Rating

object als1 {

  //0 ����Spark����
  val conf = new SparkConf().setAppName("ALS")
  val sc = new SparkContext(conf)
  Logger.getRootLogger.setLevel(Level.WARN)

  //1 ��ȡ��������
  val data = sc.textFile("ml-100k/test.data")
  val ratings = data.map(_.split(',') match {
    case Array(user, item, rate) =>
      Rating(user.toInt, item.toInt, rate.toDouble)
  })

  //2 ����ģ��
  val rank = 10
  val numIterations = 20
  val model = ALS.train(ratings, rank, numIterations, 0.01)

  //3 Ԥ����
  val usersProducts = ratings.map {
    case Rating(user, product, rate) =>
      (user, product)
  }
  val predictions =
    model.predict(usersProducts).map {
      case Rating(user, product, rate) =>
        ((user, product), rate)//rate Ԥ���
    }
  val ratesAndPreds = ratings.map {
    case Rating(user, product, rate) =>
      ((user, product), rate)//rateʵ������
  }.join(predictions)
  /**�û��������**/  
  val MSE = ratesAndPreds.map {
    //user�û�ID,product��ƷID,r1ʵ������,Ԥ������r2
    case ((user, product), (r1, r2)) =>
      val err = (r1 - r2)
      err * err //���
  }.mean()//ƽ��ֵ
  println("Mean Squared Error = " + MSE)

  //4 ����/����ģ��
  model.save(sc, "myModelPath")
  val sameModel = MatrixFactorizationModel.load(sc, "myModelPath")

}
