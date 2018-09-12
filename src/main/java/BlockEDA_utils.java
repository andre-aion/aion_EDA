import org.aion.api.IAionAPI;
import org.aion.api.type.BlockDetails;
import org.aion.api.type.ApiMsg;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BlockEDA_utils {

    static IAionAPI api = IAionAPI.init();
    ApiMsg apiMsg;
    static final Logger logger = LoggerFactory.getLogger(BlockEDA_utils.class);


    public BlockEDA_utils() {
        this.apiMsg = new ApiMsg();
        this.api = IAionAPI.init();
        api.connect("tcp://127.0.0.1:8547");
    }

    // retrieve more than 1000 blocks
    public List<BlockDetails> getAllBlocksInRange(long start, long end){
        long lastBlock = this.api.getChain().blockNumber().getObject();
        if (start < 0)
            start = 0;
        if (end > lastBlock)
            end = lastBlock;
        if (start > end || end < start)
            start = end;

        List<BlockDetails> allBlocks;
        List<BlockDetails> tempBlocks;
        long tempstart = start;
        long tempend = ((start + 999 >= end) ? end : (start + 999));;
        allBlocks = this.apiMsg.set(this.api.getAdmin().getBlockDetailsByRange(tempstart,tempend)).getObject();
        long count = 0;
        while (tempend < end){
            //reset start and end
            tempstart = tempend + 1;
            tempend = ((tempend + 1000 >= end) ? end : (tempend+ 1000));
            if(tempend > end)
            {
                break;
            }
            this.apiMsg.set(this.api.getAdmin().getBlockDetailsByRange(tempstart,tempend));
            if (this.apiMsg.isError()) {
                try {
                    throw new Exception("AionApi: ERR_CODE[" + this.apiMsg.getErrorCode() + "]: " + this.apiMsg.getErrString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                tempBlocks = this.apiMsg.getObject();
                allBlocks.addAll(tempBlocks);

            }

        }

        return allBlocks;
    }
    // exceed 1000 block limitation
    public List<BlockDetails> getAllLastNBlocks(long N){
        long lastBlock = this.api.getChain().blockNumber().getObject();
        long startBlock = lastBlock - (N -1);
        List<BlockDetails> allBlocks;
        allBlocks = this.getAllBlocksInRange(startBlock, lastBlock);
        return allBlocks;

    }

    // Enter dates in the format" "yyyy-MM-dd hh:mm:ss"
    public List<BlockDetails> getAllBlocksInDateRange(String startDate, String endDate) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd hh:mm:ss");
        Date parsedStartTimeStamp = dateFormat.parse(startDate);
        Date parsedEndTimeStamp = dateFormat.parse(endDate);

        long SecStartTime = parsedStartTimeStamp.getTime()/1000;
        long SecEndTime = parsedEndTimeStamp.getTime()/1000;

        long lastBlockNumber = this.api.getChain().blockNumber().getObject();
        long startBlockNumber = 0;
        long startBracketBlockNumber = this.findBlockByDatetimeSec(SecStartTime, startBlockNumber, lastBlockNumber, lastBlockNumber);
        long endBracketBlockNumber = this.findBlockByDatetimeSec(SecEndTime, startBlockNumber, lastBlockNumber, lastBlockNumber);

        List<BlockDetails> blks = this.getAllBlocksInRange(startBracketBlockNumber,endBracketBlockNumber);

        return blks;

    }


    public long findBlockByDatetimeSec(long datetimeSec, long startBlockNumber, long endBlockNumber, long lastBlockNumber) {
        long foundBlockNumber;
        if(startBlockNumber < 0){
            startBlockNumber = 0;
        }
        if(endBlockNumber > lastBlockNumber) {
            endBlockNumber = lastBlockNumber;
        }
        if(startBlockNumber == endBlockNumber)
        {
            return -1;
        }
        else if (startBlockNumber > endBlockNumber){
            //swap numbers if needed
            long temp = endBlockNumber;
            endBlockNumber = startBlockNumber;
            startBlockNumber = temp;
            return findBlockByDatetimeSec(datetimeSec, startBlockNumber, endBlockNumber, lastBlockNumber);
        }
        else {
            long midBlockNumber = startBlockNumber + (endBlockNumber - startBlockNumber) / 2;
            BlockDetails midBlk = this.api.getAdmin().getBlockDetailsByNumber(midBlockNumber).getObject();
            BlockDetails nextBlk = this.api.getAdmin().getBlockDetailsByNumber(midBlockNumber+1).getObject();

            if (datetimeSec < midBlk.getTimestamp()){
                endBlockNumber = midBlockNumber;
                System.out.println("calling recursive bottom half");
                return findBlockByDatetimeSec(datetimeSec, startBlockNumber, endBlockNumber, lastBlockNumber);

            }
            else if(datetimeSec > nextBlk.getTimestamp()){
                startBlockNumber = midBlockNumber+1;
                System.out.println("calling recursive top half");
                return findBlockByDatetimeSec(datetimeSec, startBlockNumber, endBlockNumber, lastBlockNumber);
            }
            else if(datetimeSec >= midBlk.getTimestamp() && datetimeSec <= nextBlk.getTimestamp()) {
                return midBlockNumber;
            }

        }
        return -1;
    }

    public static void main(String[] args) {

        BlockEDA_utils blkexp = new BlockEDA_utils();
        //List<BlockDetails> blks = blkexp.getAllLastNBlocks(7500);

        //search by date range
        try {
            List<BlockDetails> blks = blkexp.getAllBlocksInDateRange("2018-08-01 00:00:00", "2018-09-04 00:00:00");
            for (BlockDetails blk : blks){
                System.out.println(blk.getNumber());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
