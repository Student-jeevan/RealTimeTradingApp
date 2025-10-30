import React from 'react';
import Sidebar from './Sidebar';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import { DragHandleHorizontalIcon} from "@radix-ui/react-icons"
import { Avatar, AvatarImage } from '@radix-ui/react-avatar';
const Navbar = ()=>{
    return (
        <div className='px-2 py-3 border-b z-50 bg-background bg-opacity-0 sticky top-0 left-0 right-0 flex justify-between items-center'>

            <div className ='flex items-center gap-3'>
                <Sheet>
                    <SheetTrigger>
                        <Button variant="ghost" size="icon" className="rounded-full h-11 w-11">
                            <DragHandleHorizontalIcon className ='h-7 w-7' />
                        </Button>
                    </SheetTrigger>
                    <SheetContent className="w-72 border-r-0 flex flex-col justify-center" side="left">
                        <SheetHeader>
                        <SheetTitle>
                            <div className="text-2xl flex justify-center items-center gap-1">
                                <Avatar>
                                    <AvatarImage src="/crypto_logo.png"  alt="Crypto App Logo - CryptEx Trade"  />
                                </Avatar>
                                <div>
                                    <span className="font-bold text-orange-700">Crypto</span>
                                    <span>Trading</span>
                                </div>
                            </div>
                        </SheetTitle>
                        
                        </SheetHeader>
                        <Sidebar/>
                    </SheetContent>
                </Sheet>
            </div>

        </div>
    )
}
export default Navbar;