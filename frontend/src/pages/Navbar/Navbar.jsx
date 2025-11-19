import React from 'react';
import Sidebar from './Sidebar';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import { DragHandleHorizontalIcon, MagnifyingGlassIcon } from "@radix-ui/react-icons"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { useSelector } from 'react-redux';

const Navbar = ()=>{
    const auth = useSelector(state => state.auth);
    return (
        <div className='px-2 py-3 border-b z-50 bg-background bg-opacity-0 sticky top-0 left-0 right-0 flex justify-between items-center'>

            <div className ='flex items-center gap-3'>
                <Sheet>
                    <SheetTrigger asChild>
                        <Button variant="ghost" size="icon" className="rounded-full h-11 w-11">
                            <DragHandleHorizontalIcon className ='h-7 w-7' />
                        </Button>
                    </SheetTrigger>

                    
                    <SheetContent className="w-72 border-r-0 flex flex-col justify-start p-4 overflow-y-auto" side="left">
                        <SheetHeader>
                            <SheetTitle>
                                <div className="text-2xl flex items-center gap-3">
                                    <Avatar className="h-10 w-10 flex-shrink-0">
                                        <AvatarImage
                                          src="https://assets.coingecko.com/coins/images/1/large/bitcoin.png"
                                          alt="Crypto App Logo - CryptEx Trade"
                                          className="h-10 w-10 object-contain filter brightness-105"
                                        />
                                    </Avatar>
                                    <div className="flex flex-col whitespace-nowrap">
                                        <span className="font-bold text-orange-700 leading-tight">Crypto</span>
                                        <span className="leading-tight">Trading</span>
                                    </div>
                                </div>
                            </SheetTitle>
                        </SheetHeader>

                        <Sidebar/>
                    </SheetContent>
                </Sheet>

                <p className='text-sm lg:text-base cursor-pointer'>
                    Crypto Trading
                </p>

                <div className="p-0 ml-9">
                    <Button variant="outline" className="flex items-center gap-3">
                        <MagnifyingGlassIcon/>
                        <span>Search</span>
                    </Button>
                </div>
            </div>

            <div>
                <Avatar>
                    <AvatarFallback>
                        {auth.user?.fullName[0].toUpperCase()}
                    </AvatarFallback>
                </Avatar>
            </div>
        </div>
    )
}
export default Navbar;