import React, { useEffect } from 'react'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table';
import { useDispatch, useSelector } from 'react-redux';
import { getUserAssets } from '@/State/Asset/Action';
function Portfolio() {
    const dispatch = useDispatch();
    const {asset} = useSelector((store)=>store)
    
    useEffect(()=>{
      const jwt = localStorage.getItem("jwt");
      if (jwt) {
        dispatch(getUserAssets(jwt));
      }
    },[dispatch])
    return (
        <div className='px-5 lg:px-20'>
            <h1 className='font-bold text-3xl pb-5'>Portfolio</h1>
             <Table className="w-full">
                  <TableHeader>
                    <TableRow>  
                      <TableHead className="w-[180px]">Asset</TableHead>
                      <TableHead className="w-[100px] text-center">Price</TableHead>
                      <TableHead className="w-[160px] text-center">Unit</TableHead>
                      <TableHead className="w-[160px] text-right">Change</TableHead>
                      <TableHead className="w-[100px] text-right">Change%</TableHead>
                      <TableHead className="w-[120px] text-right">Value </TableHead>
                    </TableRow>
                  </TableHeader>
            
                  <TableBody>
                    {asset?.userAssets && asset.userAssets.length > 0 ? (
                      asset.userAssets.map((item, index) => (
                        <TableRow key={index} className="hover:bg-muted/50">
                          <TableCell className="font-medium flex items-center gap-3">
                            <Avatar className="h-8 w-8">
                              <AvatarImage src={item?.coin?.image}/>
                              <AvatarFallback>{item?.coin?.symbol?.charAt(0)?.toUpperCase() || 'A'}</AvatarFallback>
                            </Avatar>
                            <span>{item?.coin?.name || 'N/A'}</span>
                          </TableCell>
                
                          <TableCell className="text-center">${item?.coin?.current_price || item?.coin?.price || 0}</TableCell>
                          <TableCell className="text-center">{item?.quantity || 0}</TableCell>
                          <TableCell className={`text-right ${(item?.coin?.price_change_24h || 0) >= 0 ? 'text-green-500' : 'text-red-500'}`}>
                            ${item?.coin?.price_change_24h || 0}
                          </TableCell>
                          <TableCell className={`text-right ${(item?.coin?.price_change_percentage_24h || 0) >= 0 ? 'text-green-500' : 'text-red-500'}`}>
                            {item?.coin?.price_change_percentage_24h ? `${item.coin.price_change_percentage_24h.toFixed(2)}%` : '0%'}
                          </TableCell>
                          <TableCell className="text-right font-semibold">
                            ${((item?.quantity || 0) * (item?.coin?.current_price || item?.coin?.price || 0)).toFixed(2)}
                          </TableCell>
                        </TableRow>
                      ))
                    ) : (
                      <TableRow>
                        <TableCell colSpan={6} className="text-center py-8 text-gray-500">
                          {asset?.loading ? 'Loading assets...' : 'No assets found in your portfolio'}
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
        </div>
    )
}
export default Portfolio
