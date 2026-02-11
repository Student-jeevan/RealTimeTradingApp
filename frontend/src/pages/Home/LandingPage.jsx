import React from 'react';
import { Button } from '@/components/ui/button';
import { useNavigate } from 'react-router-dom';
import { Rocket, Shield, Zap, LayoutDashboard, BarChart2, Wallet } from 'lucide-react';

const LandingPage = () => {
  const navigate = useNavigate();

  const handleGetStarted = () => {
    navigate('/signup');
  };

  const handleLogin = () => {
    navigate('/signin');
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Navbar */}
      <nav className="border-b bg-white/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <div className="flex items-center">
              <BarChart2 className="h-8 w-8 text-blue-600" />
              <span className="ml-2 text-xl font-bold text-gray-900">CryptoTrading</span>
            </div>
            <div className="flex items-center gap-4">
              <Button variant="ghost" onClick={handleLogin}>
                Login
              </Button>
              <Button onClick={handleGetStarted}>
                Sign Up
              </Button>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <div className="relative overflow-hidden bg-white">
        <div className="max-w-7xl mx-auto">
          <div className="relative z-10 pb-8 bg-white sm:pb-16 md:pb-20 lg:max-w-2xl lg:w-full lg:pb-28 xl:pb-32">
            <main className="mt-10 mx-auto max-w-7xl px-4 sm:mt-12 sm:px-6 md:mt-16 lg:mt-20 lg:px-8 xl:mt-28">
              <div className="sm:text-center lg:text-left">
                <h1 className="text-4xl tracking-tight font-extrabold text-gray-900 sm:text-5xl md:text-6xl">
                  <span className="block xl:inline">Trade crypto with</span>{' '}
                  <span className="block text-blue-600 xl:inline">confidence</span>
                </h1>
                <p className="mt-3 text-base text-gray-500 sm:mt-5 sm:text-lg sm:max-w-xl sm:mx-auto md:mt-5 md:text-xl lg:mx-0">
                  Experience the next generation of cryptocurrency trading. Real-time data, advanced analysis tools, and secure wallet management all in one place.
                </p>
                <div className="mt-5 sm:mt-8 sm:flex sm:justify-center lg:justify-start">
                  <div className="rounded-md shadow">
                    <Button onClick={handleGetStarted} className="w-full flex items-center justify-center px-8 py-3 text-base font-medium h-auto">
                      Get Started
                    </Button>
                  </div>
                  <div className="mt-3 sm:mt-0 sm:ml-3">
                    <Button variant="outline" onClick={handleLogin} className="w-full flex items-center justify-center px-8 py-3 text-base font-medium h-auto">
                      Live Demo
                    </Button>
                  </div>
                </div>
              </div>
            </main>
          </div>
        </div>
        <div className="lg:absolute lg:inset-y-0 lg:right-0 lg:w-1/2 bg-blue-50 flex items-center justify-center">
             {/* Abstract visual/placeholder for hero image */}
             <div className="relative w-full h-full flex items-center justify-center overflow-hidden">
                <div className="absolute w-96 h-96 bg-blue-400 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob"></div>
                <div className="absolute w-96 h-96 bg-purple-400 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob animation-delay-2000 top-0 right-0"></div>
                <LayoutDashboard className="w-64 h-64 text-blue-600 opacity-80 relative z-10" />
             </div>
        </div>
      </div>

      {/* Features Section */}
      <div className="py-24 bg-gray-50" id="features">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="lg:text-center">
            <h2 className="text-base text-blue-600 font-semibold tracking-wide uppercase">Features</h2>
            <p className="mt-2 text-3xl leading-8 font-extrabold tracking-tight text-gray-900 sm:text-4xl">
              Everything you need to trade
            </p>
            <p className="mt-4 max-w-2xl text-xl text-gray-500 lg:mx-auto">
              Our platform provides professional-grade tools for everyone. Whether you're a beginner or a pro, we've got you covered.
            </p>
          </div>

          <div className="mt-20">
            <div className="grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
              {/* Feature 1 */}
              <div className="pt-6">
                <div className="flow-root bg-white rounded-lg px-6 pb-8 shadow-sm hover:shadow-md transition-shadow duration-300 h-full">
                  <div className="-mt-6">
                    <div className="inline-flex items-center justify-center p-3 bg-blue-600 rounded-md shadow-lg">
                      <Zap className="h-6 w-6 text-white" aria-hidden="true" />
                    </div>
                    <h3 className="mt-8 text-lg font-medium text-gray-900 tracking-tight">Real-time Execution</h3>
                    <p className="mt-5 text-base text-gray-500">
                      Lightning fast trade execution with zero latency. Get the best prices instantly as the market moves.
                    </p>
                  </div>
                </div>
              </div>

              {/* Feature 2 */}
              <div className="pt-6">
                <div className="flow-root bg-white rounded-lg px-6 pb-8 shadow-sm hover:shadow-md transition-shadow duration-300 h-full">
                  <div className="-mt-6">
                    <div className="inline-flex items-center justify-center p-3 bg-blue-600 rounded-md shadow-lg">
                      <Shield className="h-6 w-6 text-white" aria-hidden="true" />
                    </div>
                    <h3 className="mt-8 text-lg font-medium text-gray-900 tracking-tight">Vault Security</h3>
                    <p className="mt-5 text-base text-gray-500">
                      Your assets are protected by industry-leading security protocols. We use cold storage and multi-sig wallets.
                    </p>
                  </div>
                </div>
              </div>

              {/* Feature 3 */}
              <div className="pt-6">
                <div className="flow-root bg-white rounded-lg px-6 pb-8 shadow-sm hover:shadow-md transition-shadow duration-300 h-full">
                  <div className="-mt-6">
                    <div className="inline-flex items-center justify-center p-3 bg-blue-600 rounded-md shadow-lg">
                      <Wallet className="h-6 w-6 text-white" aria-hidden="true" />
                    </div>
                    <h3 className="mt-8 text-lg font-medium text-gray-900 tracking-tight">Portfolio Management</h3>
                    <p className="mt-5 text-base text-gray-500">
                      Track your performance with advanced analytics. Manage your portfolio like a hedge fund manager.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Footer */}
      <footer className="bg-white border-t">
        <div className="max-w-7xl mx-auto py-12 px-4 sm:px-6 md:flex md:items-center md:justify-between lg:px-8">
          <div className="flex justify-center space-x-6 md:order-2">
            <span className="text-gray-400 hover:text-gray-500 cursor-pointer">
              About
            </span>
            <span className="text-gray-400 hover:text-gray-500 cursor-pointer">
              Privacy
            </span>
            <span className="text-gray-400 hover:text-gray-500 cursor-pointer">
              Terms
            </span>
          </div>
          <div className="mt-8 md:mt-0 md:order-1">
            <p className="text-center text-base text-gray-400">
              &copy; {new Date().getFullYear()} CryptoTrading, Inc. All rights reserved.
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
