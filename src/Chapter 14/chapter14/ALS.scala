package chapater14
import org.apache.log4j.{ Level, Logger }
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel
import org.apache.spark.mllib.recommendation.Rating
/**
 * ALS�ǽ�����С���˵ļ��,�ڻ���ѧϰ��ALS��ָʹ�ý�����С��������һ��Эͬ�Ƽ��㷨
 * ����ͨ���۲쵽�������û�����Ʒ���,���ƶ�ÿ���û���ϲ�ò����û��Ƽ��ʺϵĲ�Ʒ
 */
object als1 {

  def main(args: Array[String]) {

    //0 ����Spark����
    val conf = new SparkConf().setAppName("ALS").setMaster("local")
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
          ((user, product), rate)
      }
    val ratesAndPreds = ratings.map {
      case Rating(user, product, rate) =>
        ((user, product), rate)
    }.join(predictions)
    val MSE = ratesAndPreds.map {
      case ((user, product), (r1, r2)) =>
        val err = (r1 - r2)
        err * err
    }.mean()
    println("Mean Squared Error = " + MSE)

    //4 ����/����ģ��
    model.save(sc, "myModelPath")
    val sameModel = MatrixFactorizationModel.load(sc, "myModelPath")
  }
}
