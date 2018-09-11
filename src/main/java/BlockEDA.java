import org.aion.api.IAionAPI;
import org.aion.api.type.Block;
import org.aion.api.type.BlockDetails;
import org.aion.api.type.ApiMsg;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BlockEDA {

    static IAionAPI api = IAionAPI.init();
    ApiMsg apiMsg;
    static final Logger logger = LoggerFactory.getLogger(BlockExplorer.class);


    public BlockEDA() {
        this.apiMsg = new ApiMsg();
        this.api = IAionAPI.init();
        api.connect("tcp://127.0.0.1:8547");
    }

    // retrieve more than 1000 blocks
    public List<BlockDetails> getAllBlockRange(long start, long end){
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

                //System.out.println(count);
            }

        }
        for (BlockDetails blk : allBlocks){
            System.out.println(blk.getNumber());
            count++;
        }
        return allBlocks;
    }
    // exceed 1000 block limitation
    public List<BlockDetails> getAllLastNBlocks(long N){
        long lastBlock = this.api.getChain().blockNumber().getObject();
        long startBlock = lastBlock - (N -1);
        List<BlockDetails> allBlocks;
        allBlocks = this.getAllBlockRange(startBlock, lastBlock);
        return allBlocks;

    }



    public static void main(String[] args) {

        BlockEDA blkexp = new BlockEDA();
        List<BlockDetails> blks = blkexp.getAllLastNBlocks(7500);

    }
}
